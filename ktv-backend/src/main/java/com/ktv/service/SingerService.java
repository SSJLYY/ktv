package com.ktv.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.SingerDTO;
import com.ktv.entity.Singer;
import com.ktv.vo.SingerVO;

/**
 * 歌手Service
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface SingerService extends IService<Singer> {

    /**
     * 分页查询歌手列表（带筛选）
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param name 歌手名（可选）
     * @param region 地区（可选）
     * @return 分页结果
     */
    IPage<SingerVO> getSingerPage(Integer current, Integer size, String name, String region);

    /**
     * 新增歌手
     * 
     * @param singerDTO 歌手DTO
     * @return 歌手ID
     */
    Long createSinger(SingerDTO singerDTO);

    /**
     * 修改歌手
     * 
     * @param id 歌手ID
     * @param singerDTO 歌手DTO
     * @return 是否成功
     */
    Boolean updateSinger(Long id, SingerDTO singerDTO);

    /**
     * 删除歌手（逻辑删除）
     * 
     * @param id 歌手ID
     * @return 是否成功
     */
    Boolean deleteSinger(Long id);

    /**
     * 根据ID获取歌手详情
     * 
     * @param id 歌手ID
     * @return 歌手VO
     */
    SingerVO getSingerById(Long id);
}
