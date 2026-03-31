package com.ktv.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ktv.common.enums.OrderStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单VO（返回给前端的订单信息）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class OrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 包厢ID
     */
    private Long roomId;

    /**
     * 包厢名称
     */
    private String roomName;

    /**
     * 包厢类型
     */
    private String roomType;

    /**
     * 开台时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 消费时长（分钟）
     */
    private Integer durationMinutes;

    /**
     * 消费时长描述（如"1小时30分钟"）
     */
    private String durationDesc;

    /**
     * 包厢费用
     */
    private BigDecimal roomAmount;

    /**
     * 总费用
     */
    private BigDecimal totalAmount;

    /**
     * 状态：1消费中 2已结账 3已取消
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员姓名
     */
    private String operatorName;

    /**
     * 结账操作员ID
     */
    private Long closerId;

    /**
     * 结账操作员姓名
     */
    private String closerName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 获取状态描述
     */
    public String getStatusText() {
        return OrderStatusEnum.getDescription(status);
    }
}
