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
import com.ktv.entity.Category;
import com.ktv.entity.OrderSong;
import com.ktv.entity.Singer;
import com.ktv.entity.Song;
import com.ktv.mapper.CategoryMapper;
import com.ktv.mapper.OrderSongMapper;
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
    private static final int ENABLED_STATUS = 1;

    private final CategoryMapper categoryMapper;
    private final SongMapper songMapper;
    private final OrderSongMapper orderSongMapper;
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
        if (status != null && status != 0 && status != ENABLED_STATUS) {
            throw new BusinessException("状态值只能是 0 或 1");
        }

        Page<SongVO> page = new Page<>(current, size);
        return songMapper.selectPageWithConditions(page, name, singerId, categoryId, language, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSong(SongDTO songDTO) {
        String normalizedName = normalizeRequiredText(songDTO.getName(), "歌曲名称不能为空");
        Long singerId = requirePositiveSingerId(songDTO.getSingerId());
        Singer singer = loadEnabledSinger(singerId);
        Long categoryId = normalizeCategoryId(songDTO.getCategoryId());

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setName(normalizedName);
        song.setSingerId(singer.getId());
        song.setCategoryId(categoryId);
        song.setLanguage(normalizeOptionalText(song.getLanguage()));
        song.setFilePath(normalizeOptionalText(song.getFilePath()));
        song.setCoverUrl(normalizeOptionalText(song.getCoverUrl()));
        song.setLyricPath(normalizeOptionalText(song.getLyricPath()));
        song.setPinyin(PinyinUtil.getPinyin(song.getName()));
        song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));

        if (song.getLanguage() == null || song.getLanguage().isBlank()) {
            song.setLanguage("国语");
        }
        if (song.getDuration() == null) {
            song.setDuration(0);
        } else if (song.getDuration() < 0) {
            throw new BusinessException("歌曲时长不能为负数");
        }
        if (song.getIsHot() == null) {
            song.setIsHot(0);
        } else {
            validateBinaryFlag(song.getIsHot(), "热门标记");
        }
        if (song.getIsNew() == null) {
            song.setIsNew(1);
        } else {
            validateBinaryFlag(song.getIsNew(), "新歌标记");
        }
        if (song.getStatus() == null) {
            song.setStatus(ENABLED_STATUS);
        } else {
            validateBinaryFlag(song.getStatus(), "歌曲状态");
        }
        if (song.getPlayCount() == null) {
            song.setPlayCount(0);
        } else if (song.getPlayCount() < 0) {
            throw new BusinessException("播放次数不能为负数");
        }

        if (songMapper.insert(song) <= 0) {
            throw new BusinessException("歌曲创建失败");
        }

        changeSingerSongCount(singerId, 1);
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
        targetSingerId = requirePositiveSingerId(targetSingerId);
        boolean singerChanged = !Objects.equals(existSong.getSingerId(), targetSingerId);
        Singer targetSinger = singerChanged
                ? loadEnabledSinger(targetSingerId)
                : loadExistingSinger(targetSingerId);
        Long targetCategoryId = songDTO.getCategoryId() != null
                ? normalizeCategoryId(songDTO.getCategoryId())
                : existSong.getCategoryId();

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setId(id);
        song.setSingerId(targetSingerId);
        song.setCategoryId(targetCategoryId);
        song.setLanguage(songDTO.getLanguage() != null ? normalizeOptionalText(songDTO.getLanguage()) : existSong.getLanguage());
        if (song.getDuration() == null) {
            song.setDuration(existSong.getDuration());
        } else if (song.getDuration() < 0) {
            throw new BusinessException("歌曲时长不能为负数");
        }
        song.setFilePath(songDTO.getFilePath() != null ? normalizeOptionalText(songDTO.getFilePath()) : existSong.getFilePath());
        song.setCoverUrl(songDTO.getCoverUrl() != null ? normalizeOptionalText(songDTO.getCoverUrl()) : existSong.getCoverUrl());
        song.setLyricPath(songDTO.getLyricPath() != null ? normalizeOptionalText(songDTO.getLyricPath()) : existSong.getLyricPath());
        if (song.getIsHot() == null) {
            song.setIsHot(existSong.getIsHot());
        } else {
            validateBinaryFlag(song.getIsHot(), "热门标记");
        }
        if (song.getIsNew() == null) {
            song.setIsNew(existSong.getIsNew());
        } else {
            validateBinaryFlag(song.getIsNew(), "新歌标记");
        }
        if (song.getStatus() == null) {
            song.setStatus(existSong.getStatus());
        } else {
            validateBinaryFlag(song.getStatus(), "歌曲状态");
        }
        if (song.getPlayCount() == null) {
            song.setPlayCount(existSong.getPlayCount());
        } else if (song.getPlayCount() < 0) {
            throw new BusinessException("播放次数不能为负数");
        }

        if (songDTO.getName() == null || songDTO.getName().isBlank()) {
            song.setName(existSong.getName());
            song.setPinyin(existSong.getPinyin());
            song.setPinyinInitial(existSong.getPinyinInitial());
        } else {
            song.setName(normalizeRequiredText(songDTO.getName(), "歌曲名称不能为空"));
            if (!Objects.equals(existSong.getName(), song.getName())) {
                song.setPinyin(PinyinUtil.getPinyin(song.getName()));
                song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));
            } else {
                song.setPinyin(existSong.getPinyin());
                song.setPinyinInitial(existSong.getPinyinInitial());
            }
        }

        if (song.getLanguage() == null || song.getLanguage().isBlank()) {
            song.setLanguage("国语");
        }

        if (singerChanged) {
            Singer oldSinger = singerService.getById(existSong.getSingerId());
            String oldSingerName = oldSinger != null ? oldSinger.getName() : String.valueOf(existSong.getSingerId());
            log.info(
                    "歌曲歌手变更: songId={}, songName={}, oldSinger={}, newSinger={}",
                    id,
                    song.getName(),
                    oldSingerName,
                    targetSinger.getName()
            );
        }

        if (songMapper.updateById(song) <= 0) {
            throw new BusinessException("歌曲更新失败");
        }

        if (singerChanged) {
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

        Long pendingUsageCount = orderSongMapper.selectCount(new LambdaQueryWrapper<OrderSong>()
                .eq(OrderSong::getSongId, id)
                .in(OrderSong::getStatus, 0, 1));
        if (pendingUsageCount != null && pendingUsageCount > 0) {
            throw new BusinessException("该歌曲仍在待唱或播放中，暂时不能删除");
        }

        if (songMapper.deleteById(id) <= 0) {
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

    private Long requirePositiveSingerId(Long singerId) {
        if (singerId == null || singerId <= 0) {
            throw new BusinessException("歌手 ID 必须为正整数");
        }
        return singerId;
    }

    private Singer loadEnabledSinger(Long singerId) {
        Singer singer = singerService.getById(singerId);
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }
        if (singer.getStatus() == null || singer.getStatus() != ENABLED_STATUS) {
            throw new BusinessException("禁用歌手不能关联歌曲");
        }
        return singer;
    }

    private Singer loadExistingSinger(Long singerId) {
        Singer singer = singerService.getById(singerId);
        if (singer == null) {
            throw new BusinessException("姝屾墜涓嶅瓨鍦?");
        }
        return singer;
    }

    private Long normalizeCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryId <= 0) {
            throw new BusinessException("分类 ID 必须为正整数");
        }
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return categoryId;
    }

    private void validateBinaryFlag(Integer value, String fieldName) {
        if (value != 0 && value != 1) {
            throw new BusinessException(fieldName + "只能是 0 或 1");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        return value == null ? null : value.trim();
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
