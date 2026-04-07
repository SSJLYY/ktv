package com.ktv.common.i18n;

/**
 * 消息枚举：统一管理所有业务消息
 * 通过 messageKey 从 messages.properties 获取对应消息
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
public enum MessageKey {

    // ========== 通用消息 ==========
    SUCCESS("common.success"),
    OPERATION_SUCCESS("common.operation.success"),
    OPERATION_FAILED("common.operation.failed"),
    PARAM_ERROR("common.param.error"),
    NOT_FOUND("common.not.found"),
    FORBIDDEN("common.forbidden"),
    TOO_MANY_REQUESTS("common.rate.limit"),
    FILE_TOO_LARGE("common.file.too.large"),
    FILE_TYPE_ERROR("common.file.type.error"),

    // ========== 用户/认证 ==========
    LOGIN_SUCCESS("auth.login.success"),
    LOGIN_FAILED("auth.login.failed"),
    LOGIN_TOO_FREQUENT("auth.login.too.frequent"),
    USER_NOT_FOUND("auth.user.not.found"),
    PASSWORD_ERROR("auth.password.error"),
    TOKEN_EXPIRED("auth.token.expired"),
    TOKEN_INVALID("auth.token.invalid"),

    // ========== 歌曲 ==========
    SONG_NOT_FOUND("song.not.found"),
    SONG_NAME_REQUIRED("song.name.required"),
    SONG_SINGER_ID_REQUIRED("song.singer.id.required"),
    SONG_SINGER_NOT_FOUND("song.singer.not.found"),
    SONG_DELETE_SUCCESS("song.delete.success"),

    // ========== 歌手 ==========
    SINGER_NOT_FOUND("singer.not.found"),

    // ========== 订单 ==========
    ORDER_NOT_FOUND("order.not.found"),
    ORDER_CREATE_FAILED("order.create.failed"),
    ORDER_ALREADY_EXISTS("order.already.exists"),

    // ========== 包厢 ==========
    ROOM_NOT_FOUND("room.not.found"),
    ROOM_NOT_AVAILABLE("room.not.available"),
    ROOM_OPERATION_BUSY("room.operation.busy"),

    // ========== 点歌/播放 ==========
    QUEUE_SONG_NOT_FOUND("queue.song.not.found"),
    NO_SONG_PLAYING("queue.no.song.playing"),
    PLAY_DATA_ERROR("queue.play.data.error"),

    // ========== 文件安全 ==========
    FILE_SECURITY_CHECK_FAILED("security.file.check.failed"),
    FILE_NAME_INVALID("security.file.name.invalid"),
    FILE_PATH_INVALID("security.file.path.invalid"),

    ;

    private final String key;

    MessageKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
