package com.ktv.interceptor;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.ResultCode;
import com.ktv.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 用于拦截需要认证的接口，验证JWT Token的有效性
 * M25修复：优化Token解析，避免重复调用getClaimsFromToken
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token
        String token = request.getHeader(AUTHORIZATION_HEADER);

        // Token为空
        if (token == null || token.isEmpty()) {
            log.warn("JWT Token为空，请求路径：{}", request.getRequestURI());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }

        // 移除Bearer前缀
        if (token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }

        // 验证Token
        try {
            // M25修复：只解析一次Claims，避免重复调用getClaimsFromToken
            Claims claims = jwtUtil.getClaimsFromToken(token);
            
            // 检查是否过期
            if (claims.getExpiration().before(new java.util.Date())) {
                log.warn("JWT Token已过期，请求路径：{}", request.getRequestURI());
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
            }

            // Token有效，将用户信息存入请求属性
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            log.debug("JWT Token验证通过，用户：{}，角色：{}，路径：{}", username, role, request.getRequestURI());
            return true;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JWT Token验证异常，请求路径：{}，错误：{}", request.getRequestURI(), e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录状态异常，请重新登录");
        }
    }
}
