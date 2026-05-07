package com.ktv.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单基础信息 VO，用于包厢端加入校验。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBasicVO {

    /**
     * 订单 ID。
     */
    private Long id;

    /**
     * 订单编号。
     */
    private String orderNo;

    /**
     * 订单状态。
     */
    private Integer status;

    /**
     * 状态描述。
     */
    private String statusText;

    /**
     * 包厢名称。
     */
    private String roomName;
}
