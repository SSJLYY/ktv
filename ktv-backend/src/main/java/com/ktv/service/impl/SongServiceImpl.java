package com.ktv.service.impl;

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
    public IPage<SongVO> getSongPage(
            Integer current,
            Integer size,
            String name,
            Long singerId,
            Long categoryId,
            String language,
            Integer status
    ) {
        validatePageParams(current, size);
        if (singerId != null && singerId <= 0) {
            throw new BusinessException("歌手 ID 必须为正整数");
        }
        if (categoryId != null && categoryId <= 0) {
            throw new BusinessException("分类 ID 必须为正整数");
        }
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException("状态值只能是 0 或 1");
        }

        Page<SongVO> page = new Page<>(current, size);
        return songMapper.selectPageWithConditions(page, name, singerId, categoryId, language, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSong(SongDTO songDTO) {
        if (songDTO.getName() == null || songDTO.getName().isBlank()) {
            throw new BusinessException("歌曲名称不能为空");
        }
        if (songDTO.getSingerId() == null || songDTO.getSingerId() <= 0) {
            throw new BusinessException("歌手 ID 必须为正整数");
        }

        Singer singer = singerService.getById(songDTO.getSingerId());
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setPinyin(PinyinUtil.getPinyin(song.getName()));
        song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));

        if (song.getLanguage() == null || song.getLanguage().isBlank()) {
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
        if (song.getPlayCount() == null) {
            song.setPlayCount(0);
        }

        int inserted = songMapper.insert(song);
        if (inserted <= 0) {
            throw new BusinessException("歌曲创建失败");
        }

        changeSingerSongCount(songDTO.getSingerId(), 1);
        registerAfterCommit(() -> refreshSongCache(song.getId()));
        return song.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSong(Long id, SongDTO songDTO) {
        if (id == null || id <= 0) {
            throw new BusinessException("歌曲 ID 必须为正整数");
        }

        Song existSong = songMapper.selectById(id);
        if (existSong == null) {
            throw new BusinessException("歌曲不存在");
        }

        Long targetSingerId = songDTO.getSingerId() != null ? songDTO.getSingerId() : existSong.getSingerId();
        if (targetSingerId == null || targetSingerId <= 0) {
            throw new BusinessException("歌手 ID 必须为正整数");
        }

        Singer newSinger = singerService.getById(targetSingerId);
        if (newSinger == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setId(id);
        song.setSingerId(targetSingerId);

        if (song.getCategoryId() == null) {
            song.setCategoryId(existSong.getCategoryId());
        }
        if (song.getLanguage() == null) {
            song.setLanguage(existSong.getLanguage());
        }
        if (song.getDuration() == null) {
            song.setDuration(existSong.getDuration());
        }
        if (song.getFilePath() == null) {
            song.setFilePath(existSong.getFilePath());
        }
        if (song.getCoverUrl() == null) {
            song.setCoverUrl(existSong.getCoverUrl());
        }
        if (song.getLyricPath() == null) {
            song.setLyricPath(existSong.getLyricPath());
        }
        if (song.getIsHot() == null) {
            song.setIsHot(existSong.getIsHot());
        }
        if (song.getIsNew() == null) {
            song.setIsNew(existSong.getIsNew());
        }
        if (song.getStatus() == null) {
            song.setStatus(existSong.getStatus());
        }
        if (song.getPlayCount() == null) {
            song.setPlayCount(existSong.getPlayCount());
        }

        if (song.getName() == null || song.getName().isBlank()) {
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
                    "歌曲歌手变更: songId={}, songName={}, oldSinger={}, newSinger={}",
                    id,
                    song.getName(),
                    oldSingerName,
                    newSinger.getName()
            );
        }

        boolean success = songMapper.updateById(song) > 0;
        if (!success) {
            throw new BusinessException("歌曲更新失败");
        }

        if (!Objects.equals(existSong.getSingerId(), song.getSingerId())) {
            changeSingerSongCount(existSong.getSingerId(), -1);
            changeSingerSongCount(song.getSingerId(), 1);
        }

        registerAfterCommit(() -> refreshSongCache(id));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSong(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("歌曲 ID 必须为正整数");
        }

        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        boolean success = songMapper.deleteById(id) > 0;
        if (!success) {
            throw new BusinessException("歌曲删除失败");
        }

        changeSingerSongCount(song.getSingerId(), -1);
        registerAfterCommit(() -> stringRedisTemplate.delete(RedisKeyConstants.buildSongCacheKey(id)));
        return true;
    }

    @Override
    public SongVO getSongById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("歌曲 ID 必须为正整数");
        }

        String cacheKey = RedisKeyConstants.buildSongCacheKey(id);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SongVO.class);
            } catch (JsonProcessingException e) {
                log.warn("歌曲缓存反序列化失败, cacheKey={}", cacheKey);
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
            log.warn("歌曲缓存序列化失败, cacheKey={}", cacheKey);
        }
        return songVO;
    }

    @Override
    public void refreshSongCache(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("歌曲 ID 必须为正整数");
        }

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
                log.warn("歌曲缓存序列化失败, cacheKey={}", cacheKey);
            }
        }
    }

    private void validatePageParams(Integer current, Integer size) {
        if (current == null || current <= 0) {
            throw new BusinessException("页码必须大于 0");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("每页数量必须大于 0");
        }
        if (size > 100) {
            throw new BusinessException("每页数量不能超过 100");
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

    private void changeSingerSongCount(Long singerId, int delta) {
        if (singerId == null || delta == 0) {
            return;
        }

        String sql = delta > 0
                ? "song_count = song_count + " + delta
                : "song_count = GREATEST(song_count - " + (-delta) + ", 0)";
        boolean updated = singerService.update()
                .eq("id", singerId)
                .setSql(sql)
                .update();
        if (!updated) {
            throw new BusinessException("歌手歌曲数量更新失败");
        }
    }
}
