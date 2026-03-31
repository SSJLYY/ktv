package com.ktv.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
public class HealthController {

    /**
     * 健康检查接口
     *
     * @return 系统状态信息
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "KTV Backend API");
        result.put("version", "1.0.0");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("message", "系统运行正常");
        return result;
    }

}
