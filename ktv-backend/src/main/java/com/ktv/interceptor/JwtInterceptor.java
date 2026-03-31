package com.ktv.interceptor;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.ResultCode;
import com.ktv.common.util.JwtUtil; // Bug17修复：原路径 com.ktv.util.JwtUtil 不存在，正确路径是 com.ktv.common.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 * 用于拦截需要认证的接口，验证JWT Token的有效性
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
            // BugE修复：BusinessException 无 (ResultCode, String) 构造器，使用 (Integer, String) 替代
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }

        // 移除Bearer前缀
        if (token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }

        // 验证Token
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT Token无效或已过期，请求路径：{}", request.getRequestURI());
                // BugE修复：使用 (Integer, String) 构造器
                throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
            }

            // Token有效，将用户信息存入请求属性
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

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
