package com.ktv.controller.admin;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.LoginDTO;
import com.ktv.service.SysUserService;
import com.ktv.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 认证Controller
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LOGIN_RATE_LIMIT_KEY_PREFIX = "ktv:rate_limit:login:";
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 10;

    /**
     * 用户登录
     * 
     * @param loginDTO 登录DTO
     * @param request HTTP请求
     * @return 登录VO（包含JWT Token）
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO,
                                 HttpServletRequest request) {
        // 获取客户端IP
        String ip = getClientIp(request);
        
        // O2修复：添加登录IP限流，防止暴力破解
        String rateLimitKey = LOGIN_RATE_LIMIT_KEY_PREFIX + ip;
        Long attemptCount = stringRedisTemplate.opsForValue().increment(rateLimitKey);
        if (attemptCount != null && attemptCount == 1) {
            stringRedisTemplate.expire(rateLimitKey, 1, TimeUnit.MINUTES);
        }
        if (attemptCount != null && attemptCount > MAX_LOGIN_ATTEMPTS_PER_MINUTE) {
            throw new BusinessException("登录尝试过于频繁，请1分钟后再试");
        }
        
        // 执行登录
        LoginVO loginVO = sysUserService.login(loginDTO, ip);
        
        // 登录成功，清除限流计数
        stringRedisTemplate.delete(rateLimitKey);
        
        return Result.success(loginVO);
    }

    /**
     * 用户登出
     * 前端清除Token即可，无需服务端操作
     * 
     * @return 成功提示
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 前端清除Token即可，无需服务端操作
        return Result.success();
    }

    /**
     * 获取客户端IP
     * 
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
