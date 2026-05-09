package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.constant.RedisKeyConstants;
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
 * 热门歌曲服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotSongServiceImpl extends ServiceImpl<SongMapper, Song> implements HotSongService {

    private static final int WARM_UP_SIZE = 50;
    private static final int MAX_LIMIT = 100;

    private final SongMapper songMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<SongVO> getHotSongs(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        limit = Math.min(limit, MAX_LIMIT);

        Long size = stringRedisTemplate.opsForZSet().size(RedisKeyConstants.SONG_HOT);
        if (size == null || size == 0) {
            log.info("Redis 热门榜为空，开始预热");
            warmUpHotSongs();
        }

        Set<ZSetOperations.TypedTuple<String>> hotSongs = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyConstants.SONG_HOT, 0, limit - 1);
        if (hotSongs == null || hotSongs.isEmpty()) {
            log.info("Redis 热门榜为空，直接回退到数据库查询");
            return getHotSongsFromDb(limit);
        }

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
                log.warn("热门歌曲 ID 格式错误: {}", tuple.getValue());
            }
        }
        if (songIds.isEmpty()) {
            log.warn("Redis 热门榜仅包含无效歌曲 ID，回退到数据库查询");
            return getHotSongsFromDb(limit);
        }

        List<SongVO> allVos = songMapper.selectVOByIds(songIds);
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

        log.info("从 Redis 获取热门歌曲 {} 首", result.size());
        return result;
    }

    @Override
    public void incrementHotScore(Long songId) {
        if (songId == null) {
            return;
        }
        stringRedisTemplate.opsForZSet().incrementScore(RedisKeyConstants.SONG_HOT, songId.toString(), 1);
        log.info("歌曲热度 +1: songId={}", songId);
    }

    @Override
    public void warmUpHotSongs() {
        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + WARM_UP_SIZE);

        List<Song> songs = songMapper.selectList(wrapper);
        if (songs.isEmpty()) {
            log.info("数据库中没有歌曲数据，跳过热门榜预热");
            return;
        }

        stringRedisTemplate.delete(RedisKeyConstants.SONG_HOT);
        Set<ZSetOperations.TypedTuple<String>> tuples = songs.stream()
                .map(song -> new DefaultTypedTuple<>(
                        song.getId().toString(),
                        toScore(song.getPlayCount())))
                .collect(Collectors.toSet());
        stringRedisTemplate.opsForZSet().add(RedisKeyConstants.SONG_HOT, tuples);

        log.info("热门榜预热完成，共 {} 首歌曲", songs.size());
    }

    @Override
    public void syncHotScoreToDb() {
        log.info("开始同步热门分数到数据库");

        Set<ZSetOperations.TypedTuple<String>> hotSongs = stringRedisTemplate.opsForZSet()
                .rangeWithScores(RedisKeyConstants.SONG_HOT, 0, -1);
        if (hotSongs == null || hotSongs.isEmpty()) {
            log.info("Redis 热门榜为空，无需同步");
            return;
        }

        int successCount = 0;
        int failCount = 0;
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
                log.warn("同步歌曲热度失败: songId={}, error={}", tuple.getValue(), e.getMessage());
                failCount++;
            }
        }

        if (!songsToUpdate.isEmpty()) {
            this.updateBatchById(songsToUpdate);
        }

        log.info("热门分数同步完成: success={}, fail={}", successCount, failCount);
    }

    private List<SongVO> getHotSongsFromDb(Integer limit) {
        int safeLimit = Math.min(limit != null ? limit : 20, MAX_LIMIT);

        LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Song::getStatus, 1)
                .orderByDesc(Song::getPlayCount)
                .last("LIMIT " + safeLimit);

        List<Song> songs = songMapper.selectList(wrapper);
        if (songs.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> songIds = songs.stream().map(Song::getId).toList();
        List<SongVO> allVos = songMapper.selectVOByIds(songIds);
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

    private double toScore(Integer playCount) {
        return playCount != null ? playCount.doubleValue() : 0.0;
    }
}
