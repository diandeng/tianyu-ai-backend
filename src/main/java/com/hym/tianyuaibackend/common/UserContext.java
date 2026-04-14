package com.hym.tianyuaibackend.common;


/**
 * 用户上下文
 */
public class UserContext {
    public static final ThreadLocal<Long> userHolder = new ThreadLocal<>();

    public static Long getUserId() {
        return userHolder.get();
    }

    public static void setUserId(Long userId) {
        userHolder.set(userId);
    }

    public static void remove() {
        userHolder.remove();
    }
}