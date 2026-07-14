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

/**
 * 认证接口。
 *
 * <p>负责注册、登录、刷新令牌和退出登录。接口既返回 token 给前端，也把 token 写入 HttpOnly Cookie，
 * 方便支持“前端主动带 Authorization 头”和“浏览器自动携带 Cookie”两种调用方式。</p>
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    /**
     * 用户注册。
     *
     * <p>注册成功后立即签发 access/refresh token，并设置登录 Cookie，让用户无需再次登录。</p>
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<TokenResp> register(@Valid @RequestBody RegisterReq req, HttpServletResponse response) {
        TokenResp tokens = authService.register(req);
        writeTokenCookies(response, tokens);
        return Result.success(tokens);
    }

    /**
     * 用户登录。
     *
     * <p>登录成功会返回 token 并刷新 Cookie；密码校验、账号状态校验由服务层完成。</p>
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResp> login(@Valid @RequestBody LoginReq req, HttpServletResponse response) {
        TokenResp tokens = authService.login(req);
        writeTokenCookies(response, tokens);
        return Result.success(tokens);
    }

    /**
     * 刷新访问令牌。
     *
     * <p>优先读取请求体中的 refresh token；如果请求体为空，则从 {@code refresh_token} Cookie 中读取，
     * 兼容移动端/调试工具和浏览器 Cookie 登录两种场景。</p>
     */
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

    /**
     * 退出登录。
     *
     * <p>后端通过设置同名 Cookie 且 maxAge=0 让浏览器删除 access/refresh token。
     * JWT 本身仍是无状态的，如果未来需要强制立即失效，可增加黑名单或版本号机制。</p>
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletResponse response) {
        expireCookie(response, "access_token");
        expireCookie(response, "refresh_token");
        return Result.success();
    }

    /**
     * 把访问令牌和刷新令牌写入 Cookie。
     */
    private void writeTokenCookies(HttpServletResponse response, TokenResp tokens) {
        addCookie(response, "access_token", tokens.getAccessToken(), tokens.getExpiresIn());
        addCookie(response, "refresh_token", tokens.getRefreshToken(), 7 * 24 * 60 * 60L);
    }

    /**
     * 添加一个安全 Cookie。
     *
     * <p>HttpOnly 防止前端脚本读取 token；SameSite=Lax 在常规页面跳转和同站请求中可用，
     * 同时比 None 更少暴露跨站携带风险。</p>
     */
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

    /**
     * 让浏览器删除指定登录 Cookie。
     */
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
