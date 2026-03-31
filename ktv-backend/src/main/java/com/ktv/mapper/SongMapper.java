package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.Song;
import com.ktv.vo.SongVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 歌曲Mapper
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Mapper
public interface SongMapper extends BaseMapper<Song> {

    /**
     * 分页查询歌曲列表（带筛选，关联查询歌手名和分类名）
     *
     * @param page 分页对象
     * @param name 歌曲名（可选）
     * @param singerId 歌手ID（可选）
     * @param categoryId 分类ID（可选）
     * @param language 语种（可选）
     * @param status 状态（可选）：0下架 1上架，null表示全部（管理端用）
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> selectPageWithConditions(@Param("page") Page<SongVO> page,
                                           @Param("name") String name,
                                           @Param("singerId") Long singerId,
                                           @Param("categoryId") Long categoryId,
                                           @Param("language") String language,
                                           @Param("status") Integer status);

    /**
     * 按歌曲名或拼音首字母模糊搜索（包厢端用）
     *
     * @param page 分页对象
     * @param keyword 搜索关键词（歌曲名或拼音首字母）
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> searchByKeyword(@Param("page") Page<SongVO> page,
                                   @Param("keyword") String keyword);

    /**
     * 根据ID查询歌曲（关联查询歌手名和分类名，忽略status过滤）
     *
     * @param id 歌曲ID
     * @return 歌曲VO
     */
    SongVO selectVOById(@Param("id") Long id);

    /**
     * S1修复：根据ID列表批量查询歌曲（关联查询歌手名和分类名）
     *
     * @param ids 歌曲ID列表
     * @return 歌曲VO列表
     */
    List<SongVO> selectVOByIds(@Param("ids") List<Long> ids);

    /**
     * 按歌手查询歌曲（分页）
     *
     * @param page 分页对象
     * @param singerId 歌手ID
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> selectBySingerId(@Param("page") Page<SongVO> page,
                                    @Param("singerId") Long singerId);

    /**
     * 按分类查询歌曲（分页）
     *
     * @param page 分页对象
     * @param categoryId 分类ID
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> selectByCategoryId(@Param("page") Page<SongVO> page,
                                      @Param("categoryId") Long categoryId);
}
