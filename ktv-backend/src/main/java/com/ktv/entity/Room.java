package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ktv.common.enums.RoomStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 包厢实体，对应表 `t_room`。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_room")
public class Room extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 包厢名称，例如 `A01`、`豪华 1 号`。
     */
    private String name;

    /**
     * 包厢类型，例如小包、中包、大包、豪华包。
     */
    private String type;

    /**
     * 容纳人数。
     */
    private Integer capacity;

    /**
     * 每小时价格。
     */
    @TableField("price_per_hour")
    private BigDecimal pricePerHour;

    /**
     * 最低消费。
     */
    @TableField("min_consumption")
    private BigDecimal minConsumption;

    /**
     * 状态：0 空闲，1 使用中，2 清洁中，3 维护中。
     */
    private Integer status;

    /**
     * 描述。
     */
    private String description;

    /**
     * 当前订单 ID，仅用于业务查询，不持久化。
     */
    @TableField(exist = false)
    private Long currentOrderId;

    public String getStatusText() {
        return RoomStatusEnum.getDescription(status);
    }

    public boolean isAvailable() {
        return status != null && status == RoomStatusEnum.AVAILABLE.getCode();
    }

    public boolean isInUse() {
        return status != null && status == RoomStatusEnum.IN_USE.getCode();
    }
}
