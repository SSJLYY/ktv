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
        assertCategoryNameUnique(categoryDTO.getName(), null);

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }

        int inserted = categoryMapper.insert(category);
        if (inserted <= 0) {
            throw new BusinessException("分类创建失败");
        }
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existCategory = loadCategory(id);
        String targetName = categoryDTO.getName() != null ? categoryDTO.getName() : existCategory.getName();
        assertCategoryNameUnique(targetName, id);

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setId(id);
        if (category.getName() == null) {
            category.setName(existCategory.getName());
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(existCategory.getSortOrder());
        }
        if (category.getStatus() == null) {
            category.setStatus(existCategory.getStatus());
        }

        boolean updated = categoryMapper.updateById(category) > 0;
        if (!updated) {
            throw new BusinessException("分类更新失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCategory(Long id) {
        Category existCategory = loadCategory(id);
        Long songCount = categoryMapper.countSongsByCategoryId(id);
        if (songCount != null && songCount > 0) {
            throw new BusinessException("该分类下还有歌曲，无法删除");
        }

        boolean deleted = categoryMapper.deleteById(existCategory.getId()) > 0;
        if (!deleted) {
            throw new BusinessException("分类删除失败");
        }
        return true;
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        return convertToVO(loadCategory(id));
    }

    private Category loadCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    private void assertCategoryNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, name);
        if (excludeId != null) {
            queryWrapper.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("分类名称已存在");
        }
    }

    private CategoryVO convertToVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        BeanUtils.copyProperties(category, categoryVO);
        categoryVO.setStatusText(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用");
        return categoryVO;
    }
}
