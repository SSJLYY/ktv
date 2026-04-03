package com.ktv.controller.room;

import com.ktv.common.result.Result;
import com.ktv.service.OrderService;
import com.ktv.vo.OrderBasicVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "ktv:rate_limit:room_order:";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    /**
     * 根据订单ID查询订单基础信息（包厢端加入验证用）
     * 不需要 JWT 认证，/api/room/** 路径不被 JwtInterceptor 拦截
     * H21修复：添加基于IP的限流保护
     *
     * @param orderId 订单ID
     * @return 订单基础信息（id, status, roomName）
     */
    @GetMapping("/{orderId}")
    public Result<OrderBasicVO> getOrderInfo(@PathVariable Long orderId, HttpServletRequest request) {
        // H21修复：基于IP的限流检查
        String clientIp = getClientIp(request);
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + clientIp;
        
        Long requestCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (requestCount != null && requestCount == 1) {
            stringRedisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }
        
        if (requestCount != null && requestCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("IP限流：{} 超过限制（{}/min）", clientIp, requestCount);
            return Result.error(429, "请求过于频繁，请稍后再试");
        }
        
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
