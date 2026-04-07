package com.ktv.common.annotation;

import java.lang.annotation.*;

/**
 * API 速率限制注解
 * 基于 Redis 实现的分布式速率限制，支持按 IP 或按用户限流
 *
 * 使用示例：
 * <pre>
 *     // 限制每个 IP 每分钟最多 10 次请求
 *     @RateLimit(maxRequests = 10, windowSeconds = 60)
 *     public Result<?> someApi() { ... }
 *
 *     // 限制每个 IP 每分钟最多 5 次请求
 *     @RateLimit(maxRequests = 5, windowSeconds = 60, key = "custom_key_prefix")
 *     public Result<?> sensitiveApi() { ... }
 * </pre>
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 时间窗口内允许的最大请求数
     */
    int maxRequests() default 10;

    /**
     * 时间窗口大小（秒）
     */
    int windowSeconds() default 60;

    /**
     * 自定义限流 Key 前缀
     * 默认使用方法全路径作为前缀
     */
    String key() default "";

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
