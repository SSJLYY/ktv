package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface OrderSongMapper extends BaseMapper<OrderSong> {

    IPage<OrderSong> selectByOrderIdAndStatus(
            Page<OrderSong> page,
            @Param("orderId") Long orderId,
            @Param("status") Integer status
    );

    IPage<OrderSong> selectPlayedByOrderId(
            Page<OrderSong> page,
            @Param("orderId") Long orderId
    );

    OrderSong findSongInfoById(@Param("orderSongId") Long orderSongId);

    OrderSong findLatestDeletedByOrderIdAndSongId(
            @Param("orderId") Long orderId,
            @Param("songId") Long songId
    );

    int restoreDeletedOrderSong(
            @Param("id") Long id,
            @Param("songName") String songName,
            @Param("singerName") String singerName,
            @Param("sortOrder") Integer sortOrder,
            @Param("createTime") LocalDateTime createTime
    );

    int hardDeleteWaitingSong(
            @Param("orderSongId") Long orderSongId,
            @Param("orderId") Long orderId
    );
}
