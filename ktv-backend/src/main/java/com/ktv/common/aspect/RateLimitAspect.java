package com.ktv.common.aspect;

import com.ktv.common.annotation.RateLimit;
import com.ktv.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * API 速率限制切面
 * 基于 Redis INCR + EXPIRE 实现滑动窗口限流
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String RATE_LIMIT_KEY_PREFIX = "ktv:rate_limit:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 构建限流 Key
        String key = buildRateLimitKey(joinPoint, rateLimit);

        // Redis INCR 获取当前计数
        Long count = stringRedisTemplate.opsForValue().increment(key);

        // 首次访问时设置过期时间
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);
        }

        // 超过限流阈值则拒绝请求
        if (count != null && count > rateLimit.maxRequests()) {
            log.warn("API限流触发：key={}, count={}, max={}", key, count, rateLimit.maxRequests());
            throw new BusinessException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流 Key：{prefix}:{customKey or methodPath}:{clientIP}
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String methodKey = rateLimit.key().isEmpty()
                ? joinPoint.getSignature().getDeclaringTypeName() + ":" + joinPoint.getSignature().getName()
                : rateLimit.key();

        String clientIp = getClientIp();

        return RATE_LIMIT_KEY_PREFIX + methodKey + ":" + clientIp;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
