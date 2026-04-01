package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.util.PinyinUtil;
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

import java.util.concurrent.TimeUnit;

/**
 * 歌曲Service实现
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongServiceImpl extends ServiceImpl<SongMapper, Song> implements SongService {

    private final SongMapper songMapper;
    private final SingerService singerService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // N4修复：Redis Key 统一使用 ktv: 前缀
    private static final String SONG_CACHE_PREFIX = "ktv:song:cache:";
    private static final String SINGER_SONG_COUNT_PREFIX = "ktv:singer:songCount:";
    /**
     * 歌曲缓存TTL（1小时）
     */
    private static final long CACHE_TTL_HOURS = 1;

    @Override
    public IPage<SongVO> getSongPage(Integer current, Integer size, String name, Long singerId, Long categoryId, String language, Integer status) {
        Page<SongVO> page = new Page<>(current, size);
        return songMapper.selectPageWithConditions(page, name, singerId, categoryId, language, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSong(SongDTO songDTO) {
        // 检查歌手是否存在
        Singer singer = singerService.getById(songDTO.getSingerId());
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);

        // 自动生成拼音（使用 PinyinUtil 统一封装）
        song.setPinyin(PinyinUtil.getPinyin(song.getName()));
        song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));

        // 设置默认值
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

        // S13修复：使用 SQL 原子更新歌曲数量，避免并发时的读-改-写竞态条件
        singerService.update().eq("id", songDTO.getSingerId()).setSql("song_count = song_count + 1").update();

        // 刷新Redis缓存
        refreshSongCache(song.getId());

        return song.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSong(Long id, SongDTO songDTO) {
        Song existSong = songMapper.selectById(id);
        if (existSong == null) {
            throw new BusinessException("歌曲不存在");
        }

        // BugB2修复：singerId 为 null 时给出明确错误，而不是让 getById(null) 引发 NPE
        if (songDTO.getSingerId() == null) {
            throw new BusinessException("歌手ID不能为空");
        }

        // 检查歌手是否存在
        Singer newSinger = singerService.getById(songDTO.getSingerId());
        if (newSinger == null) {
            throw new BusinessException("歌手不存在");
        }

        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        song.setId(id);

        // Bug10修复：元数据更新不应覆盖已上传的文件路径
        // 若 DTO 中对应字段为 null，则保留数据库中的原始值
        if (song.getFilePath() == null) {
            song.setFilePath(existSong.getFilePath());
        }
        if (song.getCoverUrl() == null) {
            song.setCoverUrl(existSong.getCoverUrl());
        }
        if (song.getLyricPath() == null) {
            song.setLyricPath(existSong.getLyricPath());
        }

        // 如果修改了歌曲名，更新拼音
        // BugD3修复：song.getName() 可能为 null（@Valid 的 @NotBlank 仅在 Create 分组生效，
        // 直接调 PUT 接口不传 name 时会 NPE）；使用 existSong.getName().equals() 时加 null 防御
        if (song.getName() == null) {
            // 未传歌曲名则保留原名，拼音也不更新
            song.setName(existSong.getName());
            song.setPinyin(existSong.getPinyin());
            song.setPinyinInitial(existSong.getPinyinInitial());
        } else if (!existSong.getName().equals(song.getName())) {
            song.setPinyin(PinyinUtil.getPinyin(song.getName()));
            song.setPinyinInitial(PinyinUtil.getPinyinInitial(song.getName()));
        } else {
            song.setPinyin(existSong.getPinyin());
            song.setPinyinInitial(existSong.getPinyinInitial());
        }

        // 如果修改了歌手，更新两个歌手的歌曲数量（S13修复：使用 SQL 原子更新）
        if (!existSong.getSingerId().equals(song.getSingerId())) {
            singerService.update().eq("id", existSong.getSingerId())
                    .setSql("song_count = GREATEST(song_count - 1, 0)").update();
            singerService.update().eq("id", song.getSingerId())
                    .setSql("song_count = song_count + 1").update();
        }

        Boolean success = songMapper.updateById(song) > 0;

        // 刷新Redis缓存
        if (success) {
            refreshSongCache(id);
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

        // S13修复：使用 SQL 原子更新歌曲数量
        singerService.update().eq("id", song.getSingerId())
                .setSql("song_count = GREATEST(song_count - 1, 0)").update();

        // MyBatis-Plus逻辑删除
        Boolean success = songMapper.deleteById(id) > 0;

        // 清除Redis缓存
        if (success) {
            String cacheKey = SONG_CACHE_PREFIX + id;
            stringRedisTemplate.delete(cacheKey);
        }

        return success;
    }

    @Override
    public SongVO getSongById(Long id) {
        // 先从Redis缓存获取（使用StringRedisTemplate + JSON序列化，避免Jackson反序列化ClassCastException）
        String cacheKey = SONG_CACHE_PREFIX + id;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SongVO.class);
            } catch (JsonProcessingException e) {
                log.warn("Redis缓存反序列化失败，cacheKey={}，将从数据库重新加载", cacheKey);
                stringRedisTemplate.delete(cacheKey);
            }
        }

        // 从数据库按ID查询（含关联歌手名、分类名）
        SongVO songVO = songMapper.selectVOById(id);
        if (songVO == null) {
            throw new BusinessException("歌曲不存在");
        }

        // 存入Redis缓存（带TTL，JSON字符串格式）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(songVO), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("Redis缓存序列化失败，cacheKey={}", cacheKey);
        }

        return songVO;
    }

    /**
     * 刷新歌曲缓存（带TTL）
     *
     * @param id 歌曲ID
     */
    @Override
    public void refreshSongCache(Long id) {
        String cacheKey = SONG_CACHE_PREFIX + id;
        // 先清除旧缓存
        stringRedisTemplate.delete(cacheKey);
        // 重新从数据库加载并写入缓存
        SongVO songVO = songMapper.selectVOById(id);
        if (songVO != null) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(songVO), CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (JsonProcessingException e) {
                log.warn("Redis缓存序列化失败，cacheKey={}", cacheKey);
            }
        }
    }
}
