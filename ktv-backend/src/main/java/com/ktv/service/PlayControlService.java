package com.ktv.service;

import com.ktv.dto.CurrentPlayVO;

/**
 * 播放控制 Service 接口。
 */
public interface PlayControlService {

    /**
     * 切歌到下一首。
     *
     * @param orderId 订单 ID
     */
    void next(Long orderId);

    /**
     * 重唱当前歌曲。
     *
     * @param orderId 订单 ID
     */
    void replay(Long orderId);

    /**
     * 暂停播放。
     *
     * @param orderId 订单 ID
     */
    void pause(Long orderId);

    /**
     * 继续播放。
     *
     * @param orderId 订单 ID
     */
    void resume(Long orderId);

    /**
     * 查询当前播放状态。
     *
     * @param orderId 订单 ID
     * @return 当前播放状态
     */
    CurrentPlayVO getCurrentPlayStatus(Long orderId);
}
