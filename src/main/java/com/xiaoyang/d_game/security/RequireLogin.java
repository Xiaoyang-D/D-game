package com.xiaoyang.d_game.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
/**
 * 标记接口需要登录。
 *
 * <p>可放在 Controller 类或具体方法上。被标记的接口必须携带有效 access token，
 * 拦截器会校验用户存在且账号未封禁，然后把当前用户写入 {@link UserContext}。</p>
 */
public @interface RequireLogin {
}
