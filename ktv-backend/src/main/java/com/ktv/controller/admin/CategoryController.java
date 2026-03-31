package com.ktv.controller.admin;

import com.ktv.common.result.Result;
import com.ktv.dto.CategoryDTO;
import com.ktv.service.CategoryService;
import com.ktv.vo.CategoryVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 歌曲分类管理Controller
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有启用的分类列表（按sort_order排序）
     * 用于下拉选择等场景
     *
     * @return 分类列表
     */
    @GetMapping
    public Result<List<CategoryVO>> getCategoryList() {
        List<CategoryVO> list = categoryService.getEnabledCategoryList();
        return Result.success(list);
    }

    /**
     * 获取所有分类列表（管理员用，包含禁用的）
     *
     * @return 分类列表
     */
    @GetMapping("/all")
    public Result<List<CategoryVO>> getAllCategoryList() {
        List<CategoryVO> list = categoryService.getAllCategoryList();
        return Result.success(list);
    }

    /**
     * 根据ID获取分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    public Result<CategoryVO> getCategoryById(@PathVariable Long id) {
        CategoryVO categoryVO = categoryService.getCategoryById(id);
        return Result.success(categoryVO);
    }

    /**
     * 新增分类
     *
     * @param categoryDTO 分类DTO
     * @return 分类ID
     */
    @PostMapping
    public Result<Long> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        Long id = categoryService.createCategory(categoryDTO);
        return Result.success(id);
    }

    /**
     * 修改分类
     *
     * @param id 分类ID
     * @param categoryDTO 分类DTO
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateCategory(@PathVariable Long id,
                                          @Valid @RequestBody CategoryDTO categoryDTO) {
        Boolean success = categoryService.updateCategory(id, categoryDTO);
        return Result.success(success);
    }

    /**
     * 删除分类（若分类下有歌曲则不可删除）
     *
     * @param id 分类ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCategory(@PathVariable Long id) {
        Boolean success = categoryService.deleteCategory(id);
        return Result.success(success);
    }
}
