package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.common.exception.BusinessException;
import com.ktv.dto.CategoryDTO;
import com.ktv.entity.Category;
import com.ktv.mapper.CategoryMapper;
import com.ktv.service.CategoryService;
import com.ktv.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 歌曲分类服务实现。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> getEnabledCategoryList() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder)
                .orderByDesc(Category::getId);
        return categoryMapper.selectList(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getAllCategoryList() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSortOrder)
                .orderByDesc(Category::getId);
        return categoryMapper.selectList(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryDTO categoryDTO) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, categoryDTO.getName());
        Long count = categoryMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("分类名称已存在");
        }

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existCategory = categoryMapper.selectById(id);
        if (existCategory == null) {
            throw new BusinessException("分类不存在");
        }

        if (categoryDTO.getName() == null) {
            categoryDTO.setName(existCategory.getName());
        }
        if (!existCategory.getName().equals(categoryDTO.getName())) {
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Category::getName, categoryDTO.getName());
            queryWrapper.ne(Category::getId, id);
            Long count = categoryMapper.selectCount(queryWrapper);
            if (count != null && count > 0) {
                throw new BusinessException("分类名称已存在");
            }
        }

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setId(id);
        return categoryMapper.updateById(category) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCategory(Long id) {
        Category existCategory = categoryMapper.selectById(id);
        if (existCategory == null) {
            throw new BusinessException("分类不存在");
        }

        Long songCount = categoryMapper.countSongsByCategoryId(id);
        if (songCount != null && songCount > 0) {
            throw new BusinessException("该分类下还有歌曲，无法删除");
        }

        return categoryMapper.deleteById(id) > 0;
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return convertToVO(category);
    }

    private CategoryVO convertToVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        BeanUtils.copyProperties(category, categoryVO);
        categoryVO.setStatusText(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用");
        return categoryVO;
    }
}
