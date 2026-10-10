package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.dto.LoginReq;
import com.xiaoyang.d_game.dto.RefreshTokenReq;
import com.xiaoyang.d_game.dto.RegisterReq;
import com.xiaoyang.d_game.dto.TokenResp;
import com.xiaoyang.d_game.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.xiaoyang.d_game.config.EmailAuthProperties emailProperties;

    private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Real-IP");
        if (emailProperties.getTrustedProxies().contains(peer) && forwarded != null
                && forwarded.length() <= 45 && forwarded.matches("[0-9a-fA-F:.]+")) { return forwarded; }
        return peer;
    }

    @PostMapping("/email/code")
    public Result<Void> sendCode(@Valid @RequestBody com.xiaoyang.d_game.dto.EmailAuthReq.SendCode req,
                                jakarta.servlet.http.HttpServletRequest request) {
        authService.sendEmailCode(req, clientIp(request)); return Result.success();
    }
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody com.xiaoyang.d_game.dto.EmailAuthReq.ResetPassword req) {
        authService.resetPassword(req); return Result.success();
    }
    @PostMapping("/migration/verify")
    public Result<com.xiaoyang.d_game.dto.EmailAuthReq.MigrationToken> verifyMigration(
            @Valid @RequestBody com.xiaoyang.d_game.dto.EmailAuthReq.VerifyMigration req,
            jakarta.servlet.http.HttpServletRequest request) {
        return Result.success(authService.verifyMigration(req, clientIp(request)));
    }
    @PostMapping("/migration/email/code")
    public Result<Void> migrationCode(@Valid @RequestBody com.xiaoyang.d_game.dto.EmailAuthReq.MigrationEmail req,
                                     jakarta.servlet.http.HttpServletRequest request) {
        authService.sendMigrationCode(req, clientIp(request)); return Result.success();
    }
    @PostMapping("/migration/bind")
    public Result<TokenResp> bindMigration(@Valid @RequestBody com.xiaoyang.d_game.dto.EmailAuthReq.BindMigration req) {
        return Result.success(authService.bindMigration(req));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<TokenResp> register(@Valid @RequestBody RegisterReq req) {
        return Result.success(authService.register(req));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenResp> login(@Valid @RequestBody LoginReq req) {
        return Result.success(authService.login(req));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<TokenResp> refresh(@Valid @RequestBody RefreshTokenReq req) {
        return Result.success(authService.refresh(req));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    /** 通过 Bearer access token 和请求体中的 refresh token 执行即时注销。 */
    public Result<Void> logout(@RequestHeader("Authorization") String authorization,
                               @Valid @RequestBody RefreshTokenReq req) {
        authService.logout(extractBearerToken(authorization), req);
        return Result.success();
    }

    /** 从请求头提取 Bearer token，拒绝其他认证格式。 */
    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return authorization.substring(7);
    }
}
