package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 点歌记录Mapper接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Mapper
public interface OrderSongMapper extends BaseMapper<OrderSong> {

    /**
     * 分页查询某订单的点歌列表
     *
     * @param page    分页对象
     * @param orderId 订单ID
     * @param status  状态（可选）
     * @return 分页点歌列表
     */
    IPage<OrderSong> selectByOrderIdAndStatus(
            Page<OrderSong> page,
            @Param("orderId") Long orderId,
            @Param("status") Integer status
    );

    /**
     * 查询已唱列表（status=2已播放 OR status=3已跳过），按 finish_time 倒序
     * Bug9修复：原来用 selectByOrderIdAndStatus(status=null) 会把等待中/播放中的歌也查出来
     *
     * @param page    分页对象
     * @param orderId 订单ID
     * @return 已唱歌曲分页列表（倒序）
     */
    IPage<OrderSong> selectPlayedByOrderId(
            Page<OrderSong> page,
            @Param("orderId") Long orderId
    );

    /**
     * 根据ID查询点歌记录（关联歌曲信息）
     *
     * @param orderSongId 点歌记录ID
     * @return 点歌记录（含歌曲时长和文件路径）
     */
    OrderSong findSongInfoById(@Param("orderSongId") Long orderSongId);
}
