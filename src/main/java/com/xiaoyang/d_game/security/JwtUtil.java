package com.xiaoyang.d_game.security;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
/**
 * JWT 生成与解析工具。
 *
 * <p>本项目区分 access token 和 refresh token：access token 用于接口访问鉴权，
 * refresh token 只允许调用刷新接口。令牌中携带用户 ID、用户名、角色列表和 tokenType，
 * 拦截器会根据 tokenType 防止刷新令牌被当作访问令牌使用。</p>
 */
public class JwtUtil {

    /** JWT 中标识令牌用途的自定义声明名称。 */
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** 访问令牌类型。 */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** 刷新令牌类型。 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    /**
     * 启动时校验 JWT 密钥。
     *
     * <p>JJWT 的 HMAC 密钥需要足够长度；提前失败比运行时生成/解析令牌时报错更容易定位。</p>
     */
    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(jwtProperties.getSecret()) || jwtProperties.getSecret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 characters");
        }
    }

    /**
     * 生成访问令牌。
     *
     * @param userId 用户 ID，会写入 subject
     * @param username 用户名，便于日志和前端展示
     * @param roles 用户角色编码列表，用于快速判断权限
     * @return 已签名的 JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TOKEN_TYPE_ACCESS, jwtProperties.getAccessExpireMs());
    }

    /**
     * 生成刷新令牌。
     *
     * <p>刷新令牌和访问令牌使用同一套签名算法，但 tokenType 和有效期不同。</p>
     */
    public String generateRefreshToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpireMs());
    }

    /**
     * 解析并验证令牌签名。
     *
     * <p>任何签名不匹配、格式错误或解析失败都会统一转换成业务异常，避免上层暴露底层库异常。</p>
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
    }

    /**
     * 将访问令牌转换成当前用户上下文。
     *
     * <p>这里只接受 access token；同时会检查过期时间、subject 是否能转成用户 ID，并读取角色列表。</p>
     */
    public UserContext toUserContext(String token) {
        Claims claims = parseToken(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        if (claims.getExpiration().before(new Date())) {
            throw new BizException(ResultCode.TOKEN_EXPIRED);
        }
        UserContext context = new UserContext();
        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        try {
            context.setUserId(Long.valueOf(subject));
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        context.setUsername(claims.get("username", String.class));
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        context.setRoles(roles == null ? new HashSet<>() : new HashSet<>(roles));
        return context;
    }

    /**
     * 构建并签名 JWT。
     *
     * <p>subject 固定使用用户 ID，其他展示和权限信息放在 claim 中；过期时间按传入 token 类型分别控制。</p>
     */
    private String buildToken(Long userId, String username, List<String> roles, String tokenType, long expireMs) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 根据配置密钥生成 HMAC SecretKey。
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
