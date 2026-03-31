package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ktv.common.enums.RoomStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 包厢实体类
 * 对应表：t_room
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_room")
public class Room extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 包厢名称（如：A01、豪华1号）
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
    @TableField("price_per_hour")
    private BigDecimal pricePerHour;

    /**
     * 最低消费（元）
     */
    @TableField("min_consumption")
    private BigDecimal minConsumption;

    /**
     * 状态：0空闲 1使用中 2清洁中 3维修中
     */
    private Integer status;

    /**
     * 描述
     */
    private String description;

    // ========== 非数据库字段 ==========

    /**
     * 当前订单ID（业务逻辑用，不持久化）
     */
    @TableField(exist = false)
    private Long currentOrderId;

    // ========== 便捷方法 ==========

    /**
     * 状态描述
     * N6修复：使用枚举替代魔法数字
     */
    public String getStatusText() {
        return RoomStatusEnum.getDescription(status);
    }

    /**
     * 是否可用（空闲状态）
     */
    public boolean isAvailable() {
        return status != null && status == RoomStatusEnum.AVAILABLE.getCode();
    }

    /**
     * 是否在使用中
     */
    public boolean isInUse() {
        return status != null && status == RoomStatusEnum.IN_USE.getCode();
    }
}
