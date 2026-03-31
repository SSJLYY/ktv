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
 * 歌曲分类Service实现
 *
 * @author shaun.sheng
 * @since 2026-03-30
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
        List<Category> categoryList = categoryMapper.selectList(queryWrapper);
        return categoryList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getAllCategoryList() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSortOrder)
                    .orderByDesc(Category::getId);
        List<Category> categoryList = categoryMapper.selectList(queryWrapper);
        return categoryList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryDTO categoryDTO) {
        // 检查分类名是否已存在
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getName, categoryDTO.getName());
        Long count = categoryMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("分类名称已存在");
        }

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        // 设置默认值
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

        // 检查分类名是否与其他分类重复
        // BugE2修复：categoryDTO.getName() 可能为 null（@Valid 对 Create 分组 @NotBlank 在 PUT 接口不生效），
        // existCategory.getName().equals(null) 返回 false，再进 queryWrapper.eq(name, null) 查询逻辑错误。
        // 修复：name 为 null 时保留原名，不触发重名校验。
        if (categoryDTO.getName() == null) {
            categoryDTO.setName(existCategory.getName());
        }
        if (!existCategory.getName().equals(categoryDTO.getName())) {
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Category::getName, categoryDTO.getName());
            queryWrapper.ne(Category::getId, id);
            Long count = categoryMapper.selectCount(queryWrapper);
            if (count > 0) {
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

        // 检查该分类下是否有歌曲
        Long songCount = categoryMapper.countSongsByCategoryId(id);
        if (songCount > 0) {
            throw new BusinessException("该分类下还有歌曲，无法删除");
        }

        // MyBatis-Plus逻辑删除
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

    /**
     * 将Category实体转换为CategoryVO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO categoryVO = new CategoryVO();
        BeanUtils.copyProperties(category, categoryVO);
        // 设置状态文本
        categoryVO.setStatusText(category.getStatus() != null && category.getStatus() == 1 ? "启用" : "禁用");
        return categoryVO;
    }
}
