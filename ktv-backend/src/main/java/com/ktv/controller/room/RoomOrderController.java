package com.ktv.controller.room;

import com.ktv.common.result.Result;
import com.ktv.service.OrderService;
import com.ktv.vo.OrderBasicVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 包厢端订单接口（无需认证）
 * 供包厢点歌端查询订单基础信息，用于加入包厢验证
 *
 * @author shaun.sheng
 * @since 2026-03-31
 */
@Slf4j
@RestController
@RequestMapping("/api/room/orders")
@RequiredArgsConstructor
public class RoomOrderController {

    /**
     * S6修复：移除 OrderMapper/RoomMapper 直接依赖，改用 OrderService
     * S7修复：Controller 层不应直接操作 Mapper，保持分层架构的清晰性
     */
    private final OrderService orderService;

    /**
     * 根据订单ID查询订单基础信息（包厢端加入验证用）
     * 不需要 JWT 认证，/api/room/** 路径不被 JwtInterceptor 拦截
     *
     * @param orderId 订单ID
     * @return 订单基础信息（id, status, roomName）
     */
    @GetMapping("/{orderId}")
    public Result<OrderBasicVO> getOrderInfo(@PathVariable Long orderId) {
        log.info("包厢端查询订单：orderId={}", orderId);
        OrderBasicVO result = orderService.getOrderBasicInfo(orderId);
        return Result.success(result);
    }
}
