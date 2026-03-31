package com.ktv.common.result;

/**
 * 统一返回状态码枚举
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    FAIL(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 系统错误
     */
    SYSTEM_ERROR(500, "系统错误"),

    /**
     * 业务异常
     */
    BUSINESS_ERROR(600, "业务异常"),

    /**
     * 数据已存在
     */
    DATA_EXIST(601, "数据已存在"),

    /**
     * 数据不存在
     */
    DATA_NOT_EXIST(602, "数据不存在"),

    /**
     * 账号或密码错误
     */
    LOGIN_ERROR(603, "账号或密码错误"),

    /**
     * Token无效或已过期
     */
    TOKEN_INVALID(604, "Token无效或已过期"),

    /**
     * 操作频繁
     */
    OPERATION_TOO_FREQUENT(605, "操作过于频繁，请稍后再试");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
