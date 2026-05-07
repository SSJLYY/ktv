package com.ktv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.CategoryDTO;
import com.ktv.entity.Category;
import com.ktv.vo.CategoryVO;

import java.util.List;

/**
 * 歌曲分类 Service。
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有启用的分类列表（按排序号升序）。
     *
     * @return 分类 VO 列表
     */
    List<CategoryVO> getEnabledCategoryList();

    /**
     * 获取所有分类列表（按排序号升序）。
     *
     * @return 分类 VO 列表
     */
    List<CategoryVO> getAllCategoryList();

    /**
     * 新增分类。
     *
     * @param categoryDTO 分类 DTO
     * @return 分类 ID
     */
    Long createCategory(CategoryDTO categoryDTO);

    /**
     * 修改分类。
     *
     * @param id 分类 ID
     * @param categoryDTO 分类 DTO
     * @return 是否成功
     */
    Boolean updateCategory(Long id, CategoryDTO categoryDTO);

    /**
     * 删除分类，若分类下有歌曲则不可删除。
     *
     * @param id 分类 ID
     * @return 是否成功
     */
    Boolean deleteCategory(Long id);

    /**
     * 根据 ID 获取分类详情。
     *
     * @param id 分类 ID
     * @return 分类 VO
     */
    CategoryVO getCategoryById(Long id);
}
