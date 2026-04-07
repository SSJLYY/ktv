package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 订单Mapper接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单列表
     *
     * @param page       分页对象
     * @param startDate  开始日期（可选）
     * @param endDate    结束日期（可选）
     * @param roomId     包厢ID（可选）
     * @param status     订单状态（可选）
     * @return 分页订单列表
     */
    IPage<Order> selectOrderPage(
            Page<Order> page,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("roomId") Long roomId,
            @Param("status") Integer status
    );

    /**
     * 查询某日期当天的订单最大序号
     *
     * @param dateStr 日期字符串（yyyyMMdd）
     * @return 最大序号
     */
    Integer selectMaxSeqByDate(@Param("dateStr") String dateStr);

    /**
     * 查询包厢当前进行中的订单
     *
     * @param roomId 包厢ID
     * @return 进行中的订单（无则返回null）
     */
    Order selectActiveOrderByRoomId(@Param("roomId") Long roomId);

    /**
     * C-1修复：原子结账更新，只有 status=1（消费中）的订单才能被结账
     * 使用 WHERE status=1 条件防止并发结账竞态
     *
     * @param orderId 订单ID
     * @param endTime 结账时间
     * @param durationMinutes 消费时长（分钟）
     * @param roomAmount 包厢费用
     * @param totalAmount 总费用
     * @param closerId 结账操作员ID
     * @return 影响行数（1=成功，0=状态已变更）
     */
    int atomicCloseOrder(
            @Param("orderId") Long orderId,
            @Param("endTime") LocalDateTime endTime,
            @Param("durationMinutes") Integer durationMinutes,
            @Param("roomAmount") java.math.BigDecimal roomAmount,
            @Param("totalAmount") java.math.BigDecimal totalAmount,
            @Param("closerId") Long closerId
    );

    /**
     * C-1修复：原子取消订单更新，只有 status=1（消费中）的订单才能被取消
     *
     * @param orderId 订单ID
     * @return 影响行数（1=成功，0=状态已变更）
     */
    int atomicCancelOrder(@Param("orderId") Long orderId);
}
