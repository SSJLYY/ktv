package com.ktv.controller.admin;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.CategoryDTO;
import com.ktv.service.CategoryService;
import com.ktv.util.AdminAccessUtils;
import com.ktv.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 歌曲分类管理 Controller。
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有启用的分类列表，按 sort_order 排序。
     */
    @GetMapping
    public Result<List<CategoryVO>> getCategoryList() {
        List<CategoryVO> list = categoryService.getEnabledCategoryList();
        return Result.success(list);
    }

    /**
     * 获取所有分类列表，供管理员使用。
     */
    @GetMapping("/all")
    public Result<List<CategoryVO>> getAllCategoryList() {
        List<CategoryVO> list = categoryService.getAllCategoryList();
        return Result.success(list);
    }

    /**
     * 根据 ID 获取分类详情。
     */
    @GetMapping("/{id}")
    public Result<CategoryVO> getCategoryById(@PathVariable Long id) {
        validatePositiveId(id, "分类 ID 必须为正整数");
        CategoryVO categoryVO = categoryService.getCategoryById(id);
        return Result.success(categoryVO);
    }

    /**
     * 新增分类。
     */
    @PostMapping
    public Result<Long> createCategory(
            @Validated(CategoryDTO.Create.class) @RequestBody CategoryDTO categoryDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Long id = categoryService.createCategory(categoryDTO);
        return Result.success(id);
    }

    /**
     * 修改分类。
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateCategory(
            @PathVariable Long id,
            @Validated(CategoryDTO.Update.class) @RequestBody CategoryDTO categoryDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(id, "分类 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = categoryService.updateCategory(id, categoryDTO);
        return Result.success(success);
    }

    /**
     * 删除分类；若分类下存在歌曲则禁止删除。
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCategory(
            @PathVariable Long id,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role
    ) {
        validatePositiveId(id, "分类 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = categoryService.deleteCategory(id);
        return Result.success(success);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }
}
