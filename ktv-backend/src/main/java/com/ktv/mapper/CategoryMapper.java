package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ktv.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 歌曲分类Mapper
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 检查分类下是否有歌曲
     *
     * @param categoryId 分类ID
     * @return 歌曲数量
     */
    Long countSongsByCategoryId(Long categoryId);
}
