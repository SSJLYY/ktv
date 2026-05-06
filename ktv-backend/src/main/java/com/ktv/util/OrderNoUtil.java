package com.ktv.util;

import com.ktv.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 订单号生成工具。
 */
@Component
@RequiredArgsConstructor
public class OrderNoUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DefaultRedisScript<Long> ORDER_NO_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('incr', KEYS[1])
            if current == 1 then
                redis.call('expire', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public String generateOrderNo() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "KTV" + dateStr;
        String redisKey = RedisKeyConstants.buildOrderNoKey(dateStr);

        Long seq = stringRedisTemplate.execute(
                ORDER_NO_SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(2 * 24 * 60 * 60)
        );
        int nextSeq = seq != null ? seq.intValue() : 1;
        return prefix + String.format("%06d", nextSeq);
    }
}
