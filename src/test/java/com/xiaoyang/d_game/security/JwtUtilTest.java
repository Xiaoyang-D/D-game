package com.xiaoyang.d_game.security;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-long-enough-for-hs256");
        properties.setAccessExpireMs(3_600_000L);
        properties.setRefreshExpireMs(86_400_000L);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void accessAndRefreshTokensHaveDistinctJtiAndTypes() {
        String access = jwtUtil.generateAccessToken(1L, "player", List.of("USER"));
        String refresh = jwtUtil.generateRefreshToken(1L, "player", List.of("USER"));

        var accessClaims = jwtUtil.parseAccessToken(access);
        var refreshClaims = jwtUtil.parseToken(refresh);
        assertEquals(1L, jwtUtil.getUserId(accessClaims));
        assertNotEquals(accessClaims.getId(), refreshClaims.getId());
        assertThrows(BizException.class, () -> jwtUtil.parseAccessToken(refresh));
    }

    @Test
    void invalidTokenTypeUsesTokenInvalidCode() {
        String refresh = jwtUtil.generateRefreshToken(1L, "player", List.of("USER"));
        BizException exception = assertThrows(BizException.class, () -> jwtUtil.parseAccessToken(refresh));
        assertEquals(ResultCode.TOKEN_INVALID.getCode(), exception.getCode());
    }
}
