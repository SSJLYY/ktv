package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.exception.BusinessException;
import com.ktv.entity.Category;
import com.ktv.entity.Singer;
import com.ktv.mapper.CategoryMapper;
import com.ktv.mapper.SingerMapper;
import com.ktv.mapper.SongMapper;
import com.ktv.service.SongSearchService;
import com.ktv.vo.CategoryVO;
import com.ktv.vo.SingerVO;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 歌曲检索Service实现（包厢端用）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongSearchServiceImpl implements SongSearchService {

    private final SongMapper songMapper;
    private final SingerMapper singerMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public IPage<SongVO> searchSongs(String keyword, Long pageNum, Long pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("搜索关键词不能为空");
        }

        // 不转大写：原样传入，XML中分别处理歌名（精确匹配）和拼音（UPPER转换）
        // 修复：之前toUpperCase()导致中文歌名LIKE匹配失效（"月亮" != "月亮"的大写）
        String searchKeyword = keyword.trim();

        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.searchByKeyword(page, searchKeyword);
    }

    @Override
    public IPage<SongVO> getSongsBySinger(Long singerId, Long pageNum, Long pageSize) {
        // 校验歌手是否存在
        Singer singer = singerMapper.selectById(singerId);
        if (singer == null) {
            throw new BusinessException("歌手不存在");
        }

        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.selectBySingerId(page, singerId);
    }

    @Override
    public IPage<SongVO> getSongsByCategory(Long categoryId, Long pageNum, Long pageSize) {
        // 校验分类是否存在
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.selectByCategoryId(page, categoryId);
    }

    @Override
    public List<SingerVO> getAllSingers(String pinyinInitial) {
        LambdaQueryWrapper<Singer> queryWrapper = new LambdaQueryWrapper<>();
        // 只查询启用的歌手
        queryWrapper.eq(Singer::getStatus, 1);

        // 按拼音首字母筛选
        if (pinyinInitial != null && !pinyinInitial.trim().isEmpty()) {
            queryWrapper.eq(Singer::getPinyinInitial, pinyinInitial.trim().toUpperCase());
        }

        // 按拼音排序
        queryWrapper.orderByAsc(Singer::getPinyin);

        List<Singer> singerList = singerMapper.selectList(queryWrapper);

        return singerList.stream()
                .map(this::convertSingerToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 只查询启用的分类
        queryWrapper.eq(Category::getStatus, 1);
        // 按排序号排序
        queryWrapper.orderByAsc(Category::getSortOrder);

        List<Category> categoryList = categoryMapper.selectList(queryWrapper);

        return categoryList.stream()
                .map(this::convertCategoryToVO)
                .collect(Collectors.toList());
    }

    /**
     * 将Singer实体转换为SingerVO
     */
    private SingerVO convertSingerToVO(Singer singer) {
        SingerVO vo = new SingerVO();
        BeanUtils.copyProperties(singer, vo);
        return vo;
    }

    /**
     * 将Category实体转换为CategoryVO
     */
    private CategoryVO convertCategoryToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setStatusText(category.getStatusText());
        return vo;
    }
}
