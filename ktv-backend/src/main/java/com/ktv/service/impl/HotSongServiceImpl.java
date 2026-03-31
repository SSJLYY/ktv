package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.service.HotSongService;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 热门歌曲Service实现类
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotSongServiceImpl implements HotSongService {

    private final SongMapper songMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis热门歌曲ZSet Key
     */
    private static final String HOT_SONG_KEY = "ktv:song:hot";

    /**
     * 预热时读取的歌曲数量
     */
    private static final int WARM_UP_SIZE = 50;

    /**
     * 获取热门歌曲排行榜
     */
    @Override
    public List<SongVO> getHotSongs(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }

        // 检查Redis中是否有数据
        Long size = stringRedisTemplate.opsForZSet().size(HOT_SONG_KEY);
        if (size == null || size == 0) {
            log.info("Redis热门榜为空，执行预热");
            warmUpHotSongs();
        }

        // 从Redis ZSet获取热门歌曲ID（按分数降序）
        Set<ZSetOperations.TypedTuple<String>> hotSongs = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(HOT_SONG_KEY, 0, limit - 1);

        if (hotSongs == null || hotSongs.isEmpty()) {
            log.info("Redis热门榜为空，从数据库直接查询");
            return getHotSongsFromDb(limit);
        }

        // 提取歌曲ID列表
        List<Long> songIds = hotSongs.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .collect(Collectors.toList());

        // S1修复：批量查询歌曲详情，替代循环逐条查询（消除 N+1）
        Map<Long, Double> scoreMap = hotSongs.stream()
                .collect(Collectors.toMap(
                        tuple -> Long.parseLong(tuple.getValue()),
                        tuple -> tuple.getScore() != null ? tuple.getScore() : 0.0
                ));
        List<SongVO> allVos = songMapper.selectVOByIds(songIds);
        // 按 songIds 的顺序返回（Redis ZSet 已按热度排序）
        Map<Long, SongVO> voMap = allVos.stream()
                .filter(vo -> vo.getStatus() == 1)
                .collect(Collectors.toMap(SongVO::getId, Function.identity(), (a, b) -> a));

        List<SongVO> result = new ArrayList<>();
        for (Long songId : songIds) {
            SongVO vo = voMap.get(songId);
            if (vo != null) {
                vo.setPlayCount(scoreMap.getOrDefault(songId, 0.0).intValue());
                result.add(vo);
            }
        }

        log.info("从Redis获取热门歌曲{}首", result.size());
        return result;
    }

    /**
     * 增加歌曲热度（点歌时调用）
     */
    @Override
    public void incrementHotScore(Long songId) {
        if (songId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().incrementScore(HOT_SONG_KEY, songId.toString(), 1);
        log.info("歌曲热度+1：songId={}", songId);
    }

    /**
     * 预热热门榜（从数据库读取并初始化Redis）
     */
    @Override
    public void warmUpHotSongs() {
        // 从数据库读取play_count最高的歌曲
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1) // 只预热上架的歌曲
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + WARM_UP_SIZE);

        List<Song> songs = songMapper.selectList(wrapper);

        if (songs.isEmpty()) {
            log.info("数据库中没有歌曲数据，跳过预热");
            return;
        }

        // 清空现有热门榜
        stringRedisTemplate.delete(HOT_SONG_KEY);

        // 批量写入Redis
        for (Song song : songs) {
            stringRedisTemplate.opsForZSet().add(HOT_SONG_KEY, song.getId().toString(), song.getPlayCount());
        }

        log.info("热门榜预热完成，共{}首歌曲", songs.size());
    }

    /**
     * 同步热门分数到数据库
     */
    @Override
    public void syncHotScoreToDb() {
        log.info("开始同步热门分数到数据库...");

        Set<ZSetOperations.TypedTuple<String>> hotSongs = stringRedisTemplate.opsForZSet()
                .rangeWithScores(HOT_SONG_KEY, 0, -1);

        if (hotSongs == null || hotSongs.isEmpty()) {
            log.info("Redis热门榜为空，无需同步");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (ZSetOperations.TypedTuple<String> tuple : hotSongs) {
            try {
                Long songId = Long.parseLong(tuple.getValue());
                Double score = tuple.getScore();
                if (score != null) {
                    Song song = new Song();
                    song.setId(songId);
                    song.setPlayCount(score.intValue());
                    songMapper.updateById(song);
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("同步歌曲{}热度失败：{}", tuple.getValue(), e.getMessage());
                failCount++;
            }
        }

        log.info("热门分数同步完成：成功{}首，失败{}首", successCount, failCount);
    }

    /**
     * 从数据库获取热门歌曲（兜底方案）
     * S1修复：改用 selectVOByIds 批量查询，替代循环逐条 selectVOById（消除 N+1）
     */
    private List<SongVO> getHotSongsFromDb(Integer limit) {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1) // 只返回上架的歌曲
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + limit);

        List<Song> songs = songMapper.selectList(wrapper);

        if (songs.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询VO
        List<Long> songIds = songs.stream().map(Song::getId).toList();
        List<SongVO> allVos = songMapper.selectVOByIds(songIds);

        // 按原始排序返回
        Map<Long, SongVO> voMap = allVos.stream()
                .collect(Collectors.toMap(SongVO::getId, Function.identity(), (a, b) -> a));

        List<SongVO> result = new ArrayList<>();
        for (Long songId : songIds) {
            SongVO vo = voMap.get(songId);
            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }
}
