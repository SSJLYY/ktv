package com.ktv.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;

/**
 * 点歌队列Service接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface PlayQueueService {

    /**
     * 点歌：添加歌曲到排队队列
     *
     * @param orderId 订单ID
     * @param songId  歌曲ID
     * @return 点歌记录ID
     */
    Long addSongToQueue(Long orderId, Long songId);

    /**
     * 置顶：将某首歌曲移到队列头部
     *
     * @param orderId     订单ID
     * @param orderSongId 点歌记录ID
     */
    void topSong(Long orderId, Long orderSongId);

    /**
     * 取消：从队列中移除歌曲
     *
     * @param orderId     订单ID
     * @param orderSongId 点歌记录ID
     */
    void removeSong(Long orderId, Long orderSongId);

    /**
     * 查询当前排队列表
     *
     * @param page    分页对象
     * @param orderId 订单ID
     * @return 分页排队列表
     */
    IPage<OrderSong> getQueueList(Page<OrderSong> page, Long orderId);

    /**
     * 查询已唱列表
     *
     * @param page    分页对象
     * @param orderId 订单ID
     * @return 分页已唱列表
     */
    IPage<OrderSong> getPlayedList(Page<OrderSong> page, Long orderId);
}
