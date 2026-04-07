package com.ktv.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单号生成工具
 * 格式：KTV + yyyyMMdd + 6位序号
 * 例如：KTV20260330000001
 * 
 * C5修复：使用Redis INCR实现分布式唯一订单号生成
 * 支持多节点部署，避免synchronized只在单JVM有效的问题
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Component
@RequiredArgsConstructor
public class OrderNoUtil {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 日期格式化
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /**
     * Redis key前缀
     */
    private static final String REDIS_KEY_PREFIX = "ktv:order:no:";

    /**
     * 生成下一个订单编号
     * C5修复：使用Redis INCR实现分布式唯一序号
     *
     * @return 订单编号
     */
    public String generateOrderNo() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "KTV" + dateStr;
        
        // C5修复：使用Redis INCR获取分布式唯一序号
        String redisKey = REDIS_KEY_PREFIX + dateStr;
        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        
        // 设置过期时间为2天，避免Redis内存泄漏
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(redisKey, 2, TimeUnit.DAYS);
        }
        
        int nextSeq = seq != null ? seq.intValue() : 1;

        // 格式化为6位序号
        return prefix + String.format("%06d", nextSeq);
    }
}
