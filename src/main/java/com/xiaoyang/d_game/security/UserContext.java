package com.xiaoyang.d_game.security;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private Long userId;
    private String username;
    private Set<String> roles = new HashSet<>();

    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        UserContext context = get();
        // 注意：不要调用 context.getUserId()。该名称被静态方法占用，通过实例调用也会解析到静态方法。
        return context == null ? null : context.userId;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public Long getContextUserId() {
        return userId;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
