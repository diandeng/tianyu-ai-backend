package com.hym.tianyuaibackend.common;

/**
 * 管理员上下文
 */
public class AdminContext {
    public static final ThreadLocal<Long> adminHolder = new ThreadLocal<>();

    public static Long getAdminId() {
        return adminHolder.get();
    }

    public static void setAdminId(Long adminId) {
        adminHolder.set(adminId);
    }

    public static void remove() {
        adminHolder.remove();
    }
}