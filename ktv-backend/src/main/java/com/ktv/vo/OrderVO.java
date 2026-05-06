package com.ktv.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ktv.common.enums.OrderStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应对象。
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long roomId;
    private String roomName;
    private String roomType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 消费时长，单位：分钟。
     */
    private Integer durationMinutes;

    /**
     * 消费时长描述，例如 `1小时30分钟`。
     */
    private String durationDesc;

    private BigDecimal roomAmount;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private String remark;
    private Long operatorId;
    private String operatorName;
    private Long closerId;
    private String closerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public String getStatusText() {
        return OrderStatusEnum.getDescription(status);
    }
}
