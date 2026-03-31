package com.ktv.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单基础信息VO（包厢端加入验证用）
 * S7修复：替代 Map<String,Object>，提供类型安全
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBasicVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusText;

    /**
     * 包厢名称
     */
    private String roomName;
}
