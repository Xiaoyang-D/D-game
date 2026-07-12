package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.config.JwtProperties;
import com.xiaoyang.d_game.dto.LoginReq;
import com.xiaoyang.d_game.dto.RefreshTokenReq;
import com.xiaoyang.d_game.dto.RegisterReq;
import com.xiaoyang.d_game.dto.TokenResp;
import com.xiaoyang.d_game.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<TokenResp> register(@Valid @RequestBody RegisterReq req, HttpServletResponse response) {
        TokenResp tokens = authService.register(req);
        writeTokenCookies(response, tokens);
        return Result.success(tokens);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResp> login(@Valid @RequestBody LoginReq req, HttpServletResponse response) {
        TokenResp tokens = authService.login(req);
        writeTokenCookies(response, tokens);
        return Result.success(tokens);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public Result<TokenResp> refresh(@CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
                                     @RequestBody(required = false) RefreshTokenReq body,
                                     HttpServletResponse response) {
        RefreshTokenReq req = body == null ? new RefreshTokenReq() : body;
        if (req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            req.setRefreshToken(cookieRefreshToken);
        }
        TokenResp tokens = authService.refresh(req);
        writeTokenCookies(response, tokens);
        return Result.success(tokens);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        expireCookie(response, "access_token");
        expireCookie(response, "refresh_token");
        return Result.success();
    }

    private void writeTokenCookies(HttpServletResponse response, TokenResp tokens) {
        addCookie(response, "access_token", tokens.getAccessToken(), tokens.getExpiresIn());
        addCookie(response, "refresh_token", tokens.getRefreshToken(), 7 * 24 * 60 * 60L);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void expireCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
