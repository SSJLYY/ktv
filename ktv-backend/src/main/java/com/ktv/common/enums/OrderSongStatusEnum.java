package com.ktv.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 点歌状态枚举
 * N6修复：替代状态码魔法数字
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Getter
@AllArgsConstructor
public enum OrderSongStatusEnum {

    WAITING(0, "等待中"),
    PLAYING(1, "播放中"),
    PLAYED(2, "已播放"),
    SKIPPED(3, "已跳过");

    private final int code;
    private final String description;

    /**
     * 根据状态码获取描述
     */
    public static String getDescription(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (OrderSongStatusEnum status : values()) {
            if (status.code == code) {
                return status.description;
            }
        }
        return "未知";
    }
}
