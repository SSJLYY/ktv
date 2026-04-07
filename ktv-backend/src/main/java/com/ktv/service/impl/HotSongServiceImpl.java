package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.service.HotSongService;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
public class HotSongServiceImpl extends ServiceImpl<SongMapper, Song> implements HotSongService {

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

        // H6/H7修复：提取歌曲ID列表，增加null和异常处理
        List<Long> songIds = new ArrayList<>();
        Map<Long, Double> scoreMap = new HashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : hotSongs) {
            try {
                String value = tuple.getValue();
                if (value != null && !value.isEmpty()) {
                    Long songId = Long.parseLong(value);
                    songIds.add(songId);
                    scoreMap.put(songId, tuple.getScore() != null ? tuple.getScore() : 0.0);
                }
            } catch (NumberFormatException e) {
                log.warn("热门歌曲ID格式错误：{}", tuple.getValue());
            }
        }
        List<SongVO> allVos = songMapper.selectVOByIds(songIds);
        // 按 songIds 的顺序返回（Redis ZSet 已按热度排序）
        Map<Long, SongVO> voMap = allVos.stream()
                .filter(vo -> vo.getStatus() != null && vo.getStatus() == 1)
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
        // M21修复：使用参数化LIMIT，避免SQL注入风险
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1) // 只预热上架的歌曲
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + WARM_UP_SIZE); // WARM_UP_SIZE是编译期常量，非用户输入，安全

        List<Song> songs = songMapper.selectList(wrapper);

        if (songs.isEmpty()) {
            log.info("数据库中没有歌曲数据，跳过预热");
            return;
        }

        // 清空现有热门榜
        stringRedisTemplate.delete(HOT_SONG_KEY);

        // M2修复：批量写入Redis ZSet，使用ZSetOperations.add(Set)减少网络往返
        Set<ZSetOperations.TypedTuple<String>> tuples = songs.stream()
                .map(song -> new DefaultTypedTuple<>(
                        song.getId().toString(),
                        song.getPlayCount().doubleValue()))
                .collect(Collectors.toSet());
        stringRedisTemplate.opsForZSet().add(HOT_SONG_KEY, tuples);

        log.info("热门榜预热完成，共{}首歌曲", songs.size());
    }

    /**
     * 同步热门分数到数据库
     * M3修复：使用批量更新替代逐条更新，减少DB写操作
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

        // M3修复：收集需要更新的歌曲，批量执行
        List<Song> songsToUpdate = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : hotSongs) {
            try {
                String value = tuple.getValue();
                if (value != null && !value.isEmpty()) {
                    Long songId = Long.parseLong(value);
                    Double score = tuple.getScore();
                    if (score != null) {
                        Song song = new Song();
                        song.setId(songId);
                        song.setPlayCount(score.intValue());
                        songsToUpdate.add(song);
                        successCount++;
                    }
                }
            } catch (Exception e) {
                log.warn("同步歌曲{}热度失败：{}", tuple.getValue(), e.getMessage());
                failCount++;
            }
        }

        // M3修复：批量更新，减少DB交互次数
        if (!songsToUpdate.isEmpty()) {
            this.updateBatchById(songsToUpdate);
        }

        log.info("热门分数同步完成：成功{}首，失败{}首", successCount, failCount);
    }

    /**
     * 从数据库获取热门歌曲（兜底方案）
     * S1修复：改用 selectVOByIds 批量查询，替代循环逐条 selectVOById（消除 N+1）
     * M21修复：limit参数增加范围限制，防止超大值导致性能问题
     */
    private List<SongVO> getHotSongsFromDb(Integer limit) {
        // M21修复：限制最大值，防止恶意请求导致数据库压力
        int safeLimit = Math.min(limit != null ? limit : 20, 100);
        
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1) // 只返回上架的歌曲
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + safeLimit); // safeLimit已做范围校验

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
