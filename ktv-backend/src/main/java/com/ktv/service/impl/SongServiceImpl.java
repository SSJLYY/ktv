package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.util.PinyinUtil;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.dto.SongDTO;
import com.ktv.entity.Singer;
import com.ktv.entity.Song;
import com.ktv.mapper.SongMapper;
import com.ktv.service.SingerService;
import com.ktv.service.SongService;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 歌曲服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    private static final long CACHE_TTL_HOURS = 1;

    private final SongMapper songMapper;
    private final SingerService singerService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<SongVO> getSongPage(Integer current, Integer size, String name, Long singerId, Long categoryId, String language, Integer status) {
        Page<SongVO> page = new Page<>(current, size);
        return songMapper.selectPageWithConditions(page, name, singerId, categoryId, language, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSong(SongDTO songDTO) {
        Singer singer = singerService.getById(songDTO.getSingerId());
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setPinyin(PinyinUtil.getPinyin(song.getName()));
        song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));

        if (song.getLanguage() == null || song.getLanguage().isEmpty()) {
            song.setLanguage("国语");
        }
        if (song.getDuration() == null) {
            song.setDuration(0);
        }
        if (song.getIsHot() == null) {
            song.setIsHot(0);
        }
        if (song.getIsNew() == null) {
            song.setIsNew(1);
        }
        if (song.getStatus() == null) {
            song.setStatus(1);
        }
        song.setPlayCount(0);

        songMapper.insert(song);
        singerService.update().eq("id", songDTO.getSingerId()).setSql("song_count = song_count + 1").update();
        registerAfterCommit(() -> refreshSongCache(song.getId()));
        return song.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSong(Long id, SongDTO songDTO) {
        Song existSong = songMapper.selectById(id);
        if (existSong == null) {
            throw new BusinessException("歌曲不存在");
        }
        if (songDTO.getSingerId() == null) {
            throw new BusinessException("歌手ID不能为空");
        }

        Singer newSinger = singerService.getById(songDTO.getSingerId());
        if (newSinger == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setId(id);

        if (song.getFilePath() == null) {
            song.setFilePath(existSong.getFilePath());
        }
        if (song.getCoverUrl() == null) {
            song.setCoverUrl(existSong.getCoverUrl());
        }
        if (song.getLyricPath() == null) {
            song.setLyricPath(existSong.getLyricPath());
        }

        if (song.getName() == null) {
            song.setName(existSong.getName());
            song.setPinyin(existSong.getPinyin());
            song.setPinyinInitial(existSong.getPinyinInitial());
        } else if (!Objects.equals(existSong.getName(), song.getName())) {
            song.setPinyin(PinyinUtil.getPinyin(song.getName()));
            song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));
        } else {
            song.setPinyin(existSong.getPinyin());
            song.setPinyinInitial(existSong.getPinyinInitial());
        }

        if (!Objects.equals(existSong.getSingerId(), song.getSingerId())) {
            Singer oldSinger = singerService.getById(existSong.getSingerId());
            String oldSingerName = oldSinger != null ? oldSinger.getName() : String.valueOf(existSong.getSingerId());
            log.info(
                    "歌曲[{}]歌手变更: {} -> {} (songId={}, oldSingerId={}, newSingerId={})",
                    song.getName(),
                    oldSingerName,
                    newSinger.getName(),
                    id,
                    existSong.getSingerId(),
                    song.getSingerId()
            );

            singerService.update().eq("id", existSong.getSingerId())
                    .setSql("song_count = GREATEST(song_count - 1, 0)").update();
            singerService.update().eq("id", song.getSingerId())
                    .setSql("song_count = song_count + 1").update();
        }

        boolean success = songMapper.updateById(song) > 0;
        if (success) {
            registerAfterCommit(() -> refreshSongCache(id));
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSong(Long id) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        singerService.update().eq("id", song.getSingerId())
                .setSql("song_count = GREATEST(song_count - 1, 0)").update();

        boolean success = songMapper.deleteById(id) > 0;
        if (success) {
            registerAfterCommit(() -> stringRedisTemplate.delete(RedisKeyConstants.buildSongCacheKey(id)));
        }
        return success;
    }

    @Override
    public SongVO getSongById(Long id) {
        String cacheKey = RedisKeyConstants.buildSongCacheKey(id);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SongVO.class);
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存反序列化失败，cacheKey={}，将从数据库重新加载", cacheKey);
                stringRedisTemplate.delete(cacheKey);
            }
        }

        SongVO songVO = songMapper.selectVOById(id);
        if (songVO == null) {
            throw new BusinessException("歌曲不存在");
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(songVO),
                    CACHE_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException e) {
            log.warn("Redis 缓存序列化失败，cacheKey={}", cacheKey);
        }
        return songVO;
    }

    @Override
    public void refreshSongCache(Long id) {
        String cacheKey = RedisKeyConstants.buildSongCacheKey(id);
        stringRedisTemplate.delete(cacheKey);

        SongVO songVO = songMapper.selectVOById(id);
        if (songVO != null) {
            try {
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(songVO),
                        CACHE_TTL_HOURS,
                        TimeUnit.HOURS
                );
            } catch (JsonProcessingException e) {
                log.warn("Redis 缓存序列化失败，cacheKey={}", cacheKey);
            }
        }
    }

    private void registerAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
