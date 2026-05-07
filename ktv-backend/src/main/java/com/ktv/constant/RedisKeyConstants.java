package com.ktv.constant;

/**
 * Redis Key 统一常量管理类。
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 应用基础前缀。
     */
    public static final String APP_PREFIX = "ktv";

    /**
     * 订单号序列生成：ktv:order:no:{date}
     */
    public static final String ORDER_NO_PREFIX = APP_PREFIX + ":order:no:";

    /**
     * 登录速率限制：ktv:rate_limit:login:{ip}
     */
    public static final String RATE_LIMIT_LOGIN_PREFIX = APP_PREFIX + ":rate_limit:login:";

    /**
     * 开台速率限制：ktv:rate_limit:room_order:{ip}
     */
    public static final String RATE_LIMIT_ROOM_ORDER_PREFIX = APP_PREFIX + ":rate_limit:room_order:";

    /**
     * 播放量同步任务锁：ktv:lock:play_count_sync
     */
    public static final String LOCK_PLAY_COUNT_SYNC = APP_PREFIX + ":lock:play_count_sync";

    /**
     * 分布式锁前缀：ktv:lock:
     */
    public static final String LOCK_PREFIX = APP_PREFIX + ":lock:";

    /**
     * 歌曲缓存：ktv:song:cache:{songId}
     */
    public static final String SONG_CACHE_PREFIX = APP_PREFIX + ":song:cache:";

    /**
     * 歌手歌曲数量缓存：ktv:singer:songCount:{singerId}
     */
    public static final String SINGER_SONG_COUNT_PREFIX = APP_PREFIX + ":singer:songCount:";

    /**
     * 热门歌曲排行：ktv:song:hot
     */
    public static final String SONG_HOT = APP_PREFIX + ":song:hot";

    /**
     * 房间状态：ktv:room:status
     */
    public static final String ROOM_STATUS = APP_PREFIX + ":room:status";

    /**
     * 点歌队列：ktv:queue:{orderId}
     */
    public static final String QUEUE_PREFIX = APP_PREFIX + ":queue:";

    /**
     * 当前播放歌曲：ktv:playing:{orderId}
     */
    public static final String PLAYING_PREFIX = APP_PREFIX + ":playing:";

    /**
     * 播放状态：ktv:play:status:{orderId}
     */
    public static final String PLAY_STATUS_PREFIX = APP_PREFIX + ":play:status:";

    /**
     * 房间当前订单：ktv:current_order:room:{roomId}
     */
    public static final String CURRENT_ORDER_ROOM_PREFIX = APP_PREFIX + ":current_order:room:";

    public static String buildOrderNoKey(String date) {
        return ORDER_NO_PREFIX + date;
    }

    public static String buildLoginRateLimitKey(String ip) {
        return RATE_LIMIT_LOGIN_PREFIX + ip;
    }

    public static String buildRoomOrderRateLimitKey(String ip) {
        return RATE_LIMIT_ROOM_ORDER_PREFIX + ip;
    }

    public static String buildSongCacheKey(Long songId) {
        return SONG_CACHE_PREFIX + songId;
    }

    public static String buildSingerSongCountKey(Long singerId) {
        return SINGER_SONG_COUNT_PREFIX + singerId;
    }

    public static String buildQueueKey(Long orderId) {
        return QUEUE_PREFIX + orderId;
    }

    public static String buildPlayingKey(Long orderId) {
        return PLAYING_PREFIX + orderId;
    }

    public static String buildPlayStatusKey(Long orderId) {
        return PLAY_STATUS_PREFIX + orderId;
    }

    public static String buildCurrentOrderRoomKey(Long roomId) {
        return CURRENT_ORDER_ROOM_PREFIX + roomId;
    }
}
