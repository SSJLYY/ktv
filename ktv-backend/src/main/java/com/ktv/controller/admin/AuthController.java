package com.ktv.controller.admin;

import com.ktv.common.annotation.RateLimit;
import com.ktv.common.result.Result;
import com.ktv.dto.LoginDTO;
import com.ktv.service.SysUserService;
import com.ktv.util.ClientIpUtils;
import com.ktv.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @PostMapping("/login")
    @RateLimit(maxRequests = 10, windowSeconds = 60, message = "登录尝试过于频繁，请1分钟后再试")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = ClientIpUtils.getClientIp(request);
        LoginVO loginVO = sysUserService.login(loginDTO, ip);
        return Result.success(loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
