package com.xiaoyang.d_game.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标记接口需要指定角色。
 *
 * <p>角色编码来自 {@code sys_role.role_code}，例如 {@code ADMIN}、{@code USER}。
 * 多个值表示满足任意一个即可访问。</p>
 */
public @interface RequireRole {

    /**
     * 允许访问该接口的角色编码列表。
     */
    String[] value();
}
