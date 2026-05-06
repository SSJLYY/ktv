package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ktv.common.enums.OrderStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体，对应表 `t_order`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号，例如 `KTV202603300001`。
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 包厢 ID。
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 开台时间。
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 消费时长，单位：分钟。
     */
    @TableField("duration_minutes")
    private Integer durationMinutes;

    /**
     * 包厢费用。
     */
    @TableField("room_amount")
    private BigDecimal roomAmount;

    /**
     * 总费用。
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：1 消费中，2 已结账，3 已取消。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 开台操作人 ID。
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 结账操作人 ID。
     */
    @TableField("closer_id")
    private Long closerId;

    /**
     * 包厢名称。
     */
    @TableField(exist = false)
    private String roomName;

    /**
     * 包厢类型。
     */
    @TableField(exist = false)
    private String roomType;

    /**
     * 开台操作人姓名。
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 结账操作人姓名。
     */
    @TableField(exist = false)
    private String closerName;

    public String getStatusText() {
        return OrderStatusEnum.getDescription(status);
    }

    public boolean isActive() {
        return status != null && status == OrderStatusEnum.CONSUMING.getCode();
    }

    public boolean isClosed() {
        return status != null && status == OrderStatusEnum.CLOSED.getCode();
    }
}
