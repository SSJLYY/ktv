package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌曲分类实体类
 * 对应表：t_category
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_category")
public class Category extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序序号（越小越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;

    // ========== 便捷方法 ==========

    /**
     * 状态描述
     */
    public String getStatusText() {
        return status != null && status == 1 ? "启用" : "禁用";
    }
}
