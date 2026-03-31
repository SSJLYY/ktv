package com.ktv.service;

import com.ktv.dto.CurrentPlayVO;

/**
 * 播放控制Service接口
 */
public interface PlayControlService {

    /**
     * 切歌（下一首）
     * 将当前歌曲标记为"已播放"，取队列下一首设为"播放中"
     *
     * @param orderId 订单ID
     */
    void next(Long orderId);

    /**
     * 重唱
     * 重置当前歌曲状态为"播放中"（重唱不换歌）
     *
     * @param orderId 订单ID
     */
    void replay(Long orderId);

    /**
     * 暂停播放
     * 更新Redis中播放状态为"已暂停"
     *
     * @param orderId 订单ID
     */
    void pause(Long orderId);

    /**
     * 恢复播放
     * 更新Redis中播放状态为"播放中"
     *
     * @param orderId 订单ID
     */
    void resume(Long orderId);

    /**
     * 查询当前播放状态
     * 返回当前播放歌曲信息、队列剩余数量
     *
     * @param orderId 订单ID
     * @return 当前播放状态
     */
    CurrentPlayVO getCurrentPlayStatus(Long orderId);
}
