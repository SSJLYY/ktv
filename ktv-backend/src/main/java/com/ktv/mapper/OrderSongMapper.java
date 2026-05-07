package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 点歌记录 Mapper 接口。
 */
@Mapper
public interface OrderSongMapper extends BaseMapper<OrderSong> {

    /**
     * 分页查询某个订单的点歌列表。
     *
     * @param page 分页对象
     * @param orderId 订单 ID
     * @param status 状态，可选
     * @return 分页点歌列表
     */
    IPage<OrderSong> selectByOrderIdAndStatus(
            Page<OrderSong> page,
            @Param("orderId") Long orderId,
            @Param("status") Integer status
    );

    /**
     * 查询已唱列表，包含已播放和已跳过记录，按 finish_time 倒序。
     *
     * @param page 分页对象
     * @param orderId 订单 ID
     * @return 已唱歌曲分页列表
     */
    IPage<OrderSong> selectPlayedByOrderId(
            Page<OrderSong> page,
            @Param("orderId") Long orderId
    );

    /**
     * 根据 ID 查询点歌记录，关联歌曲信息。
     *
     * @param orderSongId 点歌记录 ID
     * @return 点歌记录，包含歌曲时长和文件路径
     */
    OrderSong findSongInfoById(@Param("orderSongId") Long orderSongId);
}
