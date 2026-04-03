package com.ktv.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

/**
 * Redis配置类
 * C1/C7修复：使用安全的 ObjectMapper 配置，完全禁用 defaultTyping
 * 使用 StringRedisTemplate + 手动JSON序列化方案
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 创建安全的 ObjectMapper
     * C7修复：完全禁用 defaultTyping，不写入 @class 类型信息，防止反序列化RCE漏洞
     */
    private ObjectMapper createSafeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // C7修复：不调用 activateDefaultTyping，避免写入 @class 类型信息
        return objectMapper;
    }

    /**
     * 配置RedisTemplate
     * C1/C7修复：使用安全的序列化器配置
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // C7修复：使用安全的 ObjectMapper，不写入类型信息
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
                new GenericJackson2JsonRedisSerializer(createSafeObjectMapper());

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }

    /**
     * 配置缓存管理器
     * 用于Spring Cache注解支持
     * C1/C7修复：使用安全的 ObjectMapper 配置
     *
     * @param connectionFactory Redis连接工厂
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // C7修复：使用安全的 ObjectMapper
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = 
                new GenericJackson2JsonRedisSerializer(createSafeObjectMapper());

        // 配置缓存
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认缓存时间1小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .disableCachingNullValues(); // 不缓存null值

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * S11修复：Redis 分布式锁注册中心
     * 用于开台等需要防止并发竞态的操作
     *
     * @param connectionFactory Redis连接工厂
     * @return RedisLockRegistry
     */
    @Bean
    public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, "ktv:lock:", Duration.ofSeconds(30));
    }

    /**
     * N8修复：BCryptPasswordEncoder Bean
     * 用于密码加密和校验
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
