package com.ktv.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.Category;
import com.ktv.entity.Singer;
import com.ktv.vo.CategoryVO;
import com.ktv.vo.SingerVO;
import com.ktv.vo.SongVO;

import java.util.List;

/**
 * 歌曲检索Service接口（包厢端用）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface SongSearchService {

    /**
     * 按歌曲名或拼音首字母模糊搜索
     *
     * @param keyword 搜索关键词
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> searchSongs(String keyword, Long pageNum, Long pageSize);

    /**
     * 按歌手查询歌曲（分页）
     *
     * @param singerId 歌手ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> getSongsBySinger(Long singerId, Long pageNum, Long pageSize);

    /**
     * 按分类查询歌曲（分页）
     *
     * @param categoryId 分类ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 歌曲VO分页列表
     */
    IPage<SongVO> getSongsByCategory(Long categoryId, Long pageNum, Long pageSize);

    /**
     * 获取所有启用的歌手列表（支持按拼音首字母筛选）
     *
     * @param pinyinInitial 拼音首字母（可选）
     * @return 歌手VO列表
     */
    List<SingerVO> getAllSingers(String pinyinInitial);

    /**
     * 获取所有启用的分类列表
     *
     * @return 分类VO列表
     */
    List<CategoryVO> getAllCategories();
}
