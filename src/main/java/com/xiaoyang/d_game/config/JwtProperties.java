package com.xiaoyang.d_game.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
/**
 * JWT 配置属性。
 *
 * <p>绑定 {@code jwt.*} 配置，供 {@code JwtUtil} 生成和校验访问令牌、刷新令牌。
 * 生产环境必须提供足够复杂且至少 32 字符的 secret，否则应用启动时会被主动拦截。</p>
 */
public class JwtProperties {

    /**
     * JWT 签名密钥。
     *
     * <p>使用 HMAC 算法，长度不足会降低安全性；不要提交真实生产密钥到仓库。</p>
     */
    private String secret;

    /**
     * access token 有效期，单位毫秒。
     *
     * <p>访问令牌用于普通接口鉴权，过短会频繁刷新，过长会扩大令牌泄露后的风险窗口。</p>
     */
    private long accessExpireMs;

    /**
     * refresh token 有效期，单位毫秒。
     *
     * <p>刷新令牌只用于换取新的访问令牌，通常比 access token 长，但仍应有明确过期时间。</p>
     */
    private long refreshExpireMs;

    /**
     * 是否给登录 Cookie 加 Secure 属性。
     *
     * <p>生产 HTTPS 环境应开启；本地 HTTP 联调可关闭，否则浏览器不会携带 Cookie。</p>
     */
    private boolean cookieSecure;
}
