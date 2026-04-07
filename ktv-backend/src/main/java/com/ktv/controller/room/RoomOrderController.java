package com.ktv.controller.room;

import com.ktv.common.annotation.RateLimit;
import com.ktv.common.result.Result;
import com.ktv.service.OrderService;
import com.ktv.vo.OrderBasicVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 包厢端订单接口（无需认证）
 * 供包厢点歌端查询订单基础信息，用于加入包厢验证
 * H21修复：添加IP限流保护，防止接口被恶意调用
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/api/room/orders")
@RequiredArgsConstructor
public class RoomOrderController {

    private final OrderService orderService;

    /**
     * 根据订单ID查询订单基础信息（包厢端加入验证用）
     * 不需要 JWT 认证，/api/room/** 路径不被 JwtInterceptor 拦截
     * H21修复：添加基于IP的限流保护
     *
     * @param orderId 订单ID
     * @return 订单基础信息（id, status, roomName）
     */
    @GetMapping("/{orderId}")
    @RateLimit(maxRequests = 10, windowSeconds = 60, message = "请求过于频繁，请稍后再试")
    public Result<OrderBasicVO> getOrderInfo(@PathVariable Long orderId, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        log.info("包厢端查询订单：orderId={}, ip={}", orderId, clientIp);
        OrderBasicVO result = orderService.getOrderBasicInfo(orderId);
        return Result.success(result);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
