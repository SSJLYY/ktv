package com.ktv.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisKeyConstants 单元测试
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
class RedisKeyConstantsTest {

    @Test
    @DisplayName("buildQueueKey - 队列Key构建")
    void testBuildQueueKey() {
        assertEquals("ktv:queue:100", RedisKeyConstants.buildQueueKey(100L));
        assertEquals("ktv:queue:1", RedisKeyConstants.buildQueueKey(1L));
    }

    @Test
    @DisplayName("buildPlayingKey - 播放Key构建")
    void testBuildPlayingKey() {
        assertEquals("ktv:playing:200", RedisKeyConstants.buildPlayingKey(200L));
    }

    @Test
    @DisplayName("buildPlayStatusKey - 播放状态Key构建")
    void testBuildPlayStatusKey() {
        assertEquals("ktv:play:status:300", RedisKeyConstants.buildPlayStatusKey(300L));
    }

    @Test
    @DisplayName("buildCurrentOrderRoomKey - 当前订单Key构建")
    void testBuildCurrentOrderRoomKey() {
        assertEquals("ktv:current_order:room:5", RedisKeyConstants.buildCurrentOrderRoomKey(5L));
    }

    @Test
    @DisplayName("buildSongCacheKey - 歌曲缓存Key构建")
    void testBuildSongCacheKey() {
        assertEquals("ktv:song:cache:42", RedisKeyConstants.buildSongCacheKey(42L));
    }

    @Test
    @DisplayName("buildOrderNoKey - 订单号Key构建")
    void testBuildOrderNoKey() {
        assertEquals("ktv:order:no:20260407", RedisKeyConstants.buildOrderNoKey("20260407"));
    }

    @Test
    @DisplayName("buildLoginRateLimitKey - 登录限流Key构建")
    void testBuildLoginRateLimitKey() {
        assertEquals("ktv:rate_limit:login:192.168.1.1", RedisKeyConstants.buildLoginRateLimitKey("192.168.1.1"));
    }

    @Test
    @DisplayName("buildRoomOrderRateLimitKey - 开台限流Key构建")
    void testBuildRoomOrderRateLimitKey() {
        assertEquals("ktv:rate_limit:room_order:10.0.0.1", RedisKeyConstants.buildRoomOrderRateLimitKey("10.0.0.1"));
    }

    @Test
    @DisplayName("常量值完整性 - 确保关键常量正确")
    void testConstantsValues() {
        assertEquals("ktv:song:hot", RedisKeyConstants.SONG_HOT);
        assertEquals("ktv:room:status", RedisKeyConstants.ROOM_STATUS);
        assertEquals("ktv:lock:play_count_sync", RedisKeyConstants.LOCK_PLAY_COUNT_SYNC);
        assertEquals("ktv:lock:", RedisKeyConstants.LOCK_PREFIX);
    }
}
