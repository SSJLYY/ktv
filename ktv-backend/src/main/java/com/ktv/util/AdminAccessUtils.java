package com.ktv.util;

import com.ktv.common.exception.BusinessException;

/**
 * 管理端权限校验工具。
 */
public final class AdminAccessUtils {

    private static final String SUPER_ADMIN_ROLE = "super_admin";

    private AdminAccessUtils() {
    }

    public static void requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录，无法执行此操作");
        }
    }

    public static void requireSuperAdmin(Long userId, String role) {
        requireLogin(userId);
        if (!SUPER_ADMIN_ROLE.equals(role)) {
            throw new BusinessException("仅超级管理员可执行此操作");
        }
    }
}
