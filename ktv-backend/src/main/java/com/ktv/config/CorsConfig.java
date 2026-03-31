package com.ktv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域配置
 * 允许前端React应用跨域访问后端接口
 * S9修复：支持通过 application.yml 或环境变量 CORS_ALLOWED_ORIGINS 动态配置允许的源
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的跨域源列表，从配置文件读取，默认 localhost 开发环境
     * 生产环境可通过环境变量 CORS_ALLOWED_ORIGINS 配置，多个源用逗号分隔
     * 例如：CORS_ALLOWED_ORIGINS=https://admin.ktv.com,https://room.ktv.com
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 动态读取允许的源列表
        allowedOrigins.forEach(config::addAllowedOrigin);

        // 允许所有请求头
        config.addAllowedHeader("*");

        // 允许所有请求方法
        config.addAllowedMethod("*");

        // 允许携带凭证（如Cookie）
        config.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

}
