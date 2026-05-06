package com.ktv.controller.room;

import com.ktv.common.annotation.RateLimit;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.service.OrderService;
import com.ktv.vo.OrderBasicVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 包厢端订单接口（无需认证）
 * 用于包厢点歌端查询订单基础信息，作为加入包厢前校验。
 */
@Slf4j
@RestController
@RequestMapping("/api/room/orders")
@RequiredArgsConstructor
public class RoomOrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    @RateLimit(maxRequests = 10, windowSeconds = 60, message = "请求过于频繁，请稍后再试")
    public Result<OrderBasicVO> getOrderInfo(@PathVariable Long orderId, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.info("包厢端查询订单：orderId={}, ip={}", orderId, clientIp);

        OrderBasicVO result = orderService.getOrderBasicInfo(orderId);
        if (result == null || result.getStatus() == null || !result.getStatus().equals(OrderStatusEnum.CONSUMING.getCode())) {
            throw new BusinessException("该订单不在进行中");
        }

        return Result.success(result);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
