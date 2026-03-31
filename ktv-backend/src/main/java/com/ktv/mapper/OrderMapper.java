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
}
