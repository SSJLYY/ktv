package com.ktv.controller.room;

import com.ktv.common.annotation.RateLimit;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.service.OrderService;
import com.ktv.util.ClientIpUtils;
import com.ktv.vo.OrderBasicVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 包厢端订单查询接口。
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
        if (orderId == null || orderId <= 0) {
            throw new BusinessException("订单 ID 必须为正整数");
        }

        String clientIp = ClientIpUtils.getClientIp(request);
        log.info("包厢端查询订单: orderId={}, ip={}", orderId, clientIp);

        OrderBasicVO result = orderService.getOrderBasicInfo(orderId);
        if (result == null || result.getStatus() == null || result.getStatus() != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("当前订单未处于进行中状态");
        }

        return Result.success(result);
    }
}
