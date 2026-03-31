package com.ktv.config;

import com.ktv.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * 用于注册拦截器、跨域配置等
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/admin/**")  // 拦截所有admin接口
                .excludePathPatterns(
                        "/api/admin/login",        // 排除登录接口
                        "/api/admin/logout",       // 排除登出接口
                        "/api/health",             // 排除健康检查接口
                        "/test/**"                 // N2修复：排除测试接口（仅dev环境生效，配合@Profile("dev")）
                );
    }
}
