package com.ktv.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.OrderOpenDTO;
import com.ktv.entity.Order;
import com.ktv.vo.OrderBasicVO;
import com.ktv.vo.OrderVO;

import java.time.LocalDateTime;

/**
 * 订单Service接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface OrderService extends IService<Order> {

    /**
     * 开台（创建订单）
     *
     * @param openDTO      开台请求参数
     * @param operatorId   操作员ID
     * @return 订单ID
     */
    Long openOrder(OrderOpenDTO openDTO, Long operatorId);

    /**
     * 结账（关闭订单）
     *
     * @param orderId    订单ID
     * @param closerId   结账操作员ID
     * @return 订单VO（包含计算后的费用）
     */
    OrderVO closeOrder(Long orderId, Long closerId);

    /**
     * 分页查询订单
     *
     * @param page       分页对象
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param roomId     包厢ID（可选）
     * @param status     订单状态（可选）
     * @return 分页订单列表
     */
    IPage<OrderVO> getOrderPage(
            Page<Order> page,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long roomId,
            Integer status
    );

    /**
     * 根据ID查询订单详情
     *
     * @param orderId 订单ID
     * @return 订单VO
     */
    OrderVO getOrderById(Long orderId);

    /**
     * 取消订单
     *
     * @param orderId 订单ID
     * @return 是否成功
     */
    Boolean cancelOrder(Long orderId);

    /**
     * 获取包厢当前进行中的订单
     *
     * @param roomId 包厢ID
     * @return 进行中的订单（无则返回null）
     */
    OrderVO getActiveOrderByRoomId(Long roomId);

    /**
     * S6/S7修复：根据订单ID获取订单基础信息（包厢端加入验证用）
     * S7修复：返回强类型 OrderBasicVO 替代 Map<String,Object>，提供类型安全
     *
     * @param orderId 订单ID
     * @return 订单基础信息
     */
    OrderBasicVO getOrderBasicInfo(Long orderId);
}
