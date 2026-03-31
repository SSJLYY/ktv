package com.ktv.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 包厢状态枚举
 * N6修复：替代状态码魔法数字
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum RoomStatusEnum {

    AVAILABLE(0, "空闲"),
    IN_USE(1, "使用中"),
    CLEANING(2, "清洁中"),
    MAINTENANCE(3, "维修中");

    private final int code;
    private final String description;

    /**
     * 根据状态码获取描述
     */
    public static String getDescription(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (RoomStatusEnum status : values()) {
            if (status.code == code) {
                return status.description;
            }
        }
        return "未知";
    }
}
