package com.ktv.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 包厢VO（查询出参）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class RoomVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 包厢名称
     */
    private String name;

    /**
     * 类型：小包/中包/大包/豪华包
     */
    private String type;

    /**
     * 容纳人数
     */
    private Integer capacity;

    /**
     * 每小时价格（元）
     */
    private BigDecimal pricePerHour;

    /**
     * 最低消费（元）
     */
    private BigDecimal minConsumption;

    /**
     * 状态：0空闲 1使用中 2清洁中 3维修中
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
