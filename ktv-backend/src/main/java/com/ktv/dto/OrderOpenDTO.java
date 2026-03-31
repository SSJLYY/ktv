package com.ktv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 开台请求DTO
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
public class OrderOpenDTO {

    /**
     * 包厢ID
     */
    @NotNull(message = "包厢ID不能为空")
    @Positive(message = "包厢ID必须为正数")
    private Long roomId;

    /**
     * 备注（可选）
     */
    private String remark;

    // 注意：订单编号、开台时间、操作员ID由系统自动生成
}
