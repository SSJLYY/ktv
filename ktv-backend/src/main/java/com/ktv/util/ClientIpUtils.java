package com.ktv.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取工具。
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (isUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isUnknown(ip)) {
            ip = request.getRemoteAddr();
        }
        if (isUnknown(ip)) {
            return "unknown";
        }

        if (ip.contains(",")) {
            ip = ip.split(",", 2)[0].trim();
        }
        return ip;
    }

    private static boolean isUnknown(String value) {
        return value == null || value.isBlank() || "unknown".equalsIgnoreCase(value);
    }
}
