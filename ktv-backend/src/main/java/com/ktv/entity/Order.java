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
 * 订单实体类
 * 对应表：t_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号（如：KT202603300001）
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 包厢ID
     */
    @TableField("room_id")
    private Long roomId;

    /**
     * 开台时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 消费时长（分钟）
     */
    @TableField("duration_minutes")
    private Integer durationMinutes;

    /**
     * 包厢费用
     */
    @TableField("room_amount")
    private BigDecimal roomAmount;

    /**
     * 总费用
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 状态：1消费中 2已结账 3已取消
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 操作员ID（开台人）
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 结账操作员ID
     */
    @TableField("closer_id")
    private Long closerId;

    // ========== 非数据库字段（关联查询用） ==========

    /**
     * 包厢名称
     */
    @TableField(exist = false)
    private String roomName;

    /**
     * 包厢类型
     */
    @TableField(exist = false)
    private String roomType;

    /**
     * 操作员姓名
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 结账操作员姓名
     * S2修复：用于接收分页 JOIN 查询的 closer_name，避免 N+1 查询
     */
    @TableField(exist = false)
    private String closerName;

    // ========== 便捷方法 ==========

    /**
     * 状态描述
     * N6修复：使用枚举替代魔法数字
     */
    public String getStatusText() {
        return OrderStatusEnum.getDescription(status);
    }

    /**
     * 是否消费中
     */
    public boolean isActive() {
        return status != null && status == OrderStatusEnum.CONSUMING.getCode();
    }

    /**
     * 是否已结账
     */
    public boolean isClosed() {
        return status != null && status == OrderStatusEnum.CLOSED.getCode();
    }
}
