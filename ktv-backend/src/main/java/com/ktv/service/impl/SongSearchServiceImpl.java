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
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 歌曲搜索 Service 实现，供包厢端使用。
 */
@Service
@RequiredArgsConstructor
public class SongSearchServiceImpl implements SongSearchService {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SongMapper songMapper;
    private final SingerMapper singerMapper;
    private final CategoryMapper categoryMapper;

    @Override
    public IPage<SongVO> searchSongs(String keyword, Long pageNum, Long pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("搜索关键字不能为空");
        }
        validatePageParams(pageNum, pageSize);

        String searchKeyword = keyword.trim();
        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.searchByKeyword(page, searchKeyword);
    }

    @Override
    public IPage<SongVO> getSongsBySinger(Long singerId, Long pageNum, Long pageSize) {
        validatePositiveId(singerId, "歌手 ID 必须为正整数");
        validatePageParams(pageNum, pageSize);
        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.selectBySingerId(page, singerId);
    }

    @Override
    public IPage<SongVO> getSongsByCategory(Long categoryId, Long pageNum, Long pageSize) {
        validatePositiveId(categoryId, "分类 ID 必须为正整数");
        validatePageParams(pageNum, pageSize);
        Page<SongVO> page = new Page<>(pageNum, pageSize);
        return songMapper.selectByCategoryId(page, categoryId);
    }

    @Override
    public List<SingerVO> getAllSingers(String pinyinInitial) {
        LambdaQueryWrapper<Singer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Singer::getStatus, 1);

        if (pinyinInitial != null && !pinyinInitial.trim().isEmpty()) {
            String normalizedInitial = pinyinInitial.trim().toUpperCase();
            if (normalizedInitial.length() != 1) {
                throw new BusinessException("歌手首字母筛选条件无效");
            }
            queryWrapper.likeRight(Singer::getPinyinInitial, normalizedInitial);
        }

        queryWrapper.orderByAsc(Singer::getPinyin);
        List<Singer> singerList = singerMapper.selectList(queryWrapper);

        return singerList.stream()
                .map(this::convertSingerToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1);
        queryWrapper.orderByAsc(Category::getSortOrder);

        List<Category> categoryList = categoryMapper.selectList(queryWrapper);
        return categoryList.stream()
                .map(this::convertCategoryToVO)
                .collect(Collectors.toList());
    }

    private SingerVO convertSingerToVO(Singer singer) {
        SingerVO vo = new SingerVO();
        BeanUtils.copyProperties(singer, vo);
        return vo;
    }

    private CategoryVO convertCategoryToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setStatusText(category.getStatusText());
        return vo;
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validatePageParams(Long pageNum, Long pageSize) {
        if (pageNum == null || pageNum <= 0) {
            throw new BusinessException("分页页码必须大于 0");
        }
        if (pageSize == null || pageSize <= 0) {
            throw new BusinessException("分页大小必须大于 0");
        }
        if (pageSize > MAX_PAGE_SIZE) {
            throw new BusinessException("分页大小不能超过 100");
        }
    }
}
