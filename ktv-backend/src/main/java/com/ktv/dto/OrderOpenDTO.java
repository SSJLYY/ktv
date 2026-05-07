package com.ktv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 开台请求 DTO。
 */
@Data
public class OrderOpenDTO {

    /**
     * 包厢 ID。
     */
    @NotNull(message = "包厢 ID 不能为空")
    @Positive(message = "包厢 ID 必须为正整数")
    private Long roomId;

    /**
     * 备注，可选。
     */
    private String remark;
}
