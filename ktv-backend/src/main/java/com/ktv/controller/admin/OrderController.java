package com.ktv.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.OrderOpenDTO;
import com.ktv.entity.Order;
import com.ktv.service.OrderService;
import com.ktv.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 订单管理Controller
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final OrderService orderService;

    /**
     * 开台（创建订单）
     * POST /api/admin/orders/open
     */
    @PostMapping("/open")
    public Result<Long> openOrder(
            @Valid @RequestBody OrderOpenDTO openDTO,
            @RequestAttribute(name = "userId", required = false) Long userId
    ) {
        // S12修复：强制校验 userId，不再默认使用 1L
        if (userId == null) {
            throw new BusinessException("用户未登录，无法执行此操作");
        }
        Long orderId = orderService.openOrder(openDTO, userId);
        return Result.success(orderId);
    }

    /**
     * 结账（关闭订单）
     * POST /api/admin/orders/{id}/close
     */
    @PostMapping("/{id}/close")
    public Result<OrderVO> closeOrder(
            @PathVariable("id") Long orderId,
            @RequestAttribute(name = "userId", required = false) Long userId
    ) {
        validatePositiveId(orderId, "订单ID必须为正数");
        // S12修复：强制校验 userId
        if (userId == null) {
            throw new BusinessException("用户未登录，无法执行此操作");
        }
        OrderVO orderVO = orderService.closeOrder(orderId, userId);
        return Result.success(orderVO);
    }

    /**
     * 订单分页查询
     * GET /api/admin/orders
     */
    @GetMapping
    public Result<IPage<OrderVO>> getOrderPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer status
    ) {
        validatePageParams(current, size);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("开始时间不能晚于结束时间");
        }
        if (roomId != null) {
            validatePositiveId(roomId, "包厢ID必须为正数");
        }
        if (status != null && (status < 1 || status > 3)) {
            throw new BusinessException("订单状态参数无效");
        }
        Page<Order> page = new Page<>(current, size);
        IPage<OrderVO> result = orderService.getOrderPage(page, startDate, endDate, roomId, status);
        return Result.success(result);
    }

    /**
     * 获取订单详情
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderById(@PathVariable("id") Long orderId) {
        validatePositiveId(orderId, "订单ID必须为正数");
        OrderVO orderVO = orderService.getOrderById(orderId);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * DELETE /api/admin/orders/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> cancelOrder(@PathVariable("id") Long orderId,
                                       @RequestAttribute(name = "userId", required = false) Long userId) {
        validatePositiveId(orderId, "订单ID必须为正数");
        if (userId == null) {
            throw new BusinessException("用户未登录，无法执行此操作");
        }
        Boolean result = orderService.cancelOrder(orderId, userId);
        return Result.success(result);
    }

    /**
     * 获取包厢当前进行中的订单
     * GET /api/admin/orders/room/{roomId}/active
     */
    @GetMapping("/room/{roomId}/active")
    public Result<OrderVO> getActiveOrderByRoomId(@PathVariable("roomId") Long roomId) {
        validatePositiveId(roomId, "包厢ID必须为正数");
        OrderVO orderVO = orderService.getActiveOrderByRoomId(roomId);
        return Result.success(orderVO);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validatePageParams(Long current, Long size) {
        if (current == null || current <= 0) {
            throw new BusinessException("页码必须大于0");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("每页数量必须大于0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量不能超过100");
        }
    }
}
