package com.ktv.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 * N6修复：替代状态码魔法数字
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    CONSUMING(1, "消费中"),
    CLOSED(2, "已结账"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String description;

    /**
     * 根据状态码获取描述
     */
    public static String getDescription(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (OrderStatusEnum status : values()) {
            if (status.code == code) {
                return status.description;
            }
        }
        return "未知";
    }
}
