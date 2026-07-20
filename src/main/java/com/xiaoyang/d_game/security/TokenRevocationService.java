package com.xiaoyang.d_game.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
/** 使用 Redis 保存已注销 JWT 的 jti，令牌自然过期后由 TTL 自动清理。 */
public class TokenRevocationService {

    private static final String KEY_PREFIX = "jwt:revoked:";

    private final StringRedisTemplate stringRedisTemplate;

    /** 判断指定 jti 是否已经被注销。 */
    public boolean isRevoked(String tokenId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + tokenId));
    }

    /** 按 JWT 剩余有效期写入注销标记，避免黑名单永久增长。 */
    public void revoke(Claims claims) {
        String tokenId = claims.getId();
        Date expiration = claims.getExpiration();
        if (tokenId == null || tokenId.isBlank() || expiration == null) {
            return;
        }
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        if (remainingMillis > 0) {
            stringRedisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", Duration.ofMillis(remainingMillis));
        }
    }
}
