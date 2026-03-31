package com.ktv.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 歌曲分类VO（查询出参）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class CategoryVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 分类名称
     */
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
     * 状态描述
     */
    private String statusText;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
