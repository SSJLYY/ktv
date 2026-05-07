package com.ktv.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 包厢状态枚举。
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
