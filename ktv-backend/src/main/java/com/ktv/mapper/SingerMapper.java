package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.Singer;
import com.ktv.vo.SingerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 歌手Mapper
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Mapper
public interface SingerMapper extends BaseMapper<Singer> {

    /**
     * 分页查询歌手列表（带筛选）
     * 
     * @param page 分页对象
     * @param name 歌手名（可选）
     * @param region 地区（可选）
     * @return 歌手VO分页列表
     */
    IPage<SingerVO> selectPageWithConditions(@Param("page") Page<SingerVO> page,
                                             @Param("name") String name,
                                             @Param("region") String region);
}
