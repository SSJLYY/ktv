package com.ktv.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.SongDTO;
import com.ktv.entity.Song;
import com.ktv.vo.SongVO;

/**
 * 歌曲Service
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface SongService extends IService<Song> {

    /**
     * 分页查询歌曲列表（带筛选）
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param name 歌曲名（可选）
     * @param singerId 歌手ID（可选）
     * @param categoryId 分类ID（可选）
     * @param language 语种（可选）
     * @param status 状态（可选）：0下架 1上架，null表示全部
     * @return 分页结果
     */
    IPage<SongVO> getSongPage(Integer current, Integer size, String name, Long singerId, Long categoryId, String language, Integer status);

    /**
     * 新增歌曲
     * 
     * @param songDTO 歌曲DTO
     * @return 歌曲ID
     */
    Long createSong(SongDTO songDTO);

    /**
     * 修改歌曲
     * 
     * @param id 歌曲ID
     * @param songDTO 歌曲DTO
     * @return 是否成功
     */
    Boolean updateSong(Long id, SongDTO songDTO);

    /**
     * 删除歌曲（逻辑删除）
     * 
     * @param id 歌曲ID
     * @return 是否成功
     */
    Boolean deleteSong(Long id);

    /**
     * 根据ID获取歌曲详情
     *
     * @param id 歌曲ID
     * @return 歌曲VO
     */
    SongVO getSongById(Long id);

    /**
     * 刷新歌曲缓存
     *
     * @param id 歌曲ID
     */
    void refreshSongCache(Long id);
}
