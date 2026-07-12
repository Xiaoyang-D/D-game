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
public class JwtUtil {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(jwtProperties.getSecret()) || jwtProperties.getSecret().length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 characters");
        }
    }

    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TOKEN_TYPE_ACCESS, jwtProperties.getAccessExpireMs());
    }

    public String generateRefreshToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpireMs());
    }

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

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
