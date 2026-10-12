package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.EmailAuthReq;
import com.xiaoyang.d_game.dto.LoginReq;
import com.xiaoyang.d_game.dto.RefreshTokenReq;
import com.xiaoyang.d_game.dto.RegisterReq;
import com.xiaoyang.d_game.dto.TokenResp;

/**
 * 认证业务接口。
 *
 * <p>负责用户注册、登录和刷新令牌。实现类会处理密码哈希校验、默认角色分配、
 * 用户状态校验以及 access/refresh token 签发。</p>
 */
public interface AuthService {
    void sendEmailCode(EmailAuthReq.SendCode req, String ip);
    void resetPassword(EmailAuthReq.ResetPassword req);
    EmailAuthReq.MigrationToken verifyMigration(EmailAuthReq.VerifyMigration req, String ip);
    void sendMigrationCode(EmailAuthReq.MigrationEmail req, String ip);
    TokenResp bindMigration(EmailAuthReq.BindMigration req);

    /**
     * 注册新用户并返回登录令牌。
     */
    TokenResp register(RegisterReq req);

    /**
     * 邮箱密码登录并返回登录令牌。
     */
    TokenResp login(LoginReq req);

    /**
     * 使用 refresh token 换取新的 access/refresh token。
     */
    TokenResp refresh(RefreshTokenReq req);

    /** 注销当前 access token，并注销同一用户提交的 refresh token。 */
    void logout(String accessToken, RefreshTokenReq req);
}
