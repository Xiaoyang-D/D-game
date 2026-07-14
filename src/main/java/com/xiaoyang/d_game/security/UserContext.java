package com.xiaoyang.d_game.security;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
/**
 * 当前请求用户上下文。
 *
 * <p>拦截器在请求进入时把 JWT 中的用户信息放到 ThreadLocal，业务代码可以随时通过静态方法读取当前用户。
 * 请求结束后必须清理，否则容器线程复用时可能造成上下文泄露。</p>
 */
public class UserContext {

    /**
     * 每个请求线程独立保存一份用户上下文。
     */
    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    /** 当前登录用户 ID。 */
    private Long userId;

    /** 当前登录用户名。 */
    private String username;

    /** 当前用户拥有的角色编码集合。 */
    private Set<String> roles = new HashSet<>();

    /**
     * 设置当前请求上下文。
     */
    public static void set(UserContext context) {
        HOLDER.set(context);
    }

    /**
     * 获取当前请求上下文；游客或无有效 token 时可能为 null。
     */
    public static UserContext get() {
        return HOLDER.get();
    }

    /**
     * 快捷获取当前用户 ID。
     *
     * @return 当前用户 ID，未登录时返回 null
     */
    public static Long getUserId() {
        UserContext context = get();
        // 注意：不要调用 context.getUserId()。该名称被静态方法占用，通过实例调用也会解析到静态方法。
        return context == null ? null : context.userId;
    }

    /**
     * 清理当前请求上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取实例上的用户 ID。
     *
     * <p>由于类上已有静态 {@code getUserId()}，这里使用不同方法名，避免 Lombok 生成方法名冲突。</p>
     */
    public Long getContextUserId() {
        return userId;
    }

    /**
     * 判断当前用户是否拥有指定角色。
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
