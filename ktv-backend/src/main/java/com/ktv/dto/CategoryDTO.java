package com.ktv.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 歌曲分类DTO（新增/修改入参）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class CategoryDTO {

    /**
     * 分类名称（新增时必填）
     */
    @NotBlank(message = "分类名称不能为空", groups = Create.class)
    private String name;

    /**
     * 排序序号（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    /**
     * 新增校验组
     */
    public interface Create {
    }

    /**
     * 更新校验组
     */
    public interface Update {
    }
}
