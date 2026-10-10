package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.config.JwtProperties;
import com.xiaoyang.d_game.dto.LoginReq;
import com.xiaoyang.d_game.dto.RefreshTokenReq;
import com.xiaoyang.d_game.dto.RegisterReq;
import com.xiaoyang.d_game.dto.TokenResp;
import com.xiaoyang.d_game.entity.SysRole;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserRoleRel;
import com.xiaoyang.d_game.mapper.SysRoleMapper;
import com.xiaoyang.d_game.mapper.UserRoleRelMapper;
import com.xiaoyang.d_game.security.JwtUtil;
import com.xiaoyang.d_game.security.TokenRevocationService;
import com.xiaoyang.d_game.service.AuthService;
import com.xiaoyang.d_game.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.LocalDateTime;
import com.xiaoyang.d_game.dto.EmailAuthReq;
import com.xiaoyang.d_game.dto.EmailAuthReq.Purpose;
import com.xiaoyang.d_game.manager.EmailCodeManager;
import com.xiaoyang.d_game.mapper.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.dao.DuplicateKeyException;

@Service
@RequiredArgsConstructor
/**
 * 认证业务实现。
 *
 * <p>负责注册、登录、刷新 token 三条主链路。注册时会写用户表并分配默认 USER 角色；
 * 登录和刷新校验账号状态、邮箱验证和认证版本。</p>
 */
public class AuthServiceImpl implements AuthService {

    /** 新注册用户默认绑定的普通用户角色编码。 */
    private static final String ROLE_USER = "USER";
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> MIGRATION_ATTEMPT =
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                    "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],600) end; return n", Long.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final EmailCodeManager emailCodes;
    private final StringRedisTemplate redis;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenRevocationService tokenRevocationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 注册用户并立即签发 token。
     *
     * <p>事务覆盖用户插入和默认角色绑定，避免出现用户创建成功但角色关系丢失的半完成状态。</p>
     */
    public TokenResp register(RegisterReq req) {
        String email = EmailCodeManager.normalize(req.getEmail());
        emailCodes.consume(email, Purpose.REGISTER, "", req.getCode());
        if (userMapper.countEmailIncludingDeleted(email) > 0) {
            throw new BizException(ResultCode.CONFLICT, "无法注册，请检查邮箱或使用旧账号绑定入口");
        }
        User user = new User();
        user.setUsername("u_" + UUID.randomUUID().toString().replace("-", ""));
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname().trim());
        user.setEmail(email);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setAuthVersion(1);
        try {
            if (!userService.save(user)) { throw new BizException(ResultCode.INTERNAL_ERROR); }
        } catch (DuplicateKeyException exception) {
            throw new BizException(ResultCode.CONFLICT, "无法注册，请检查邮箱或使用旧账号绑定入口");
        }

        // 初始化脚本会创建 USER 角色；这里做空判断，保证缺少角色配置时注册本身不被阻断。
        SysRole userRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ROLE_USER)
                .last("LIMIT 1"));
        if (userRole != null) {
            UserRoleRel rel = new UserRoleRel();
            rel.setUserId(user.getId());
            rel.setRoleId(userRole.getId());
            userRoleRelMapper.insert(rel);
        }

        return buildTokenResp(user);
    }

    @Override
    /**
     * 用户登录。
     *
     * <p>邮箱未验证、账号不存在和密码错误都返回同一个错误，避免暴露账号是否存在。</p>
     */
    public TokenResp login(LoginReq req) {
        User user = findEmail(EmailCodeManager.normalize(req.getEmail()));
        if (user == null || user.getEmailVerifiedAt() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        userService.checkUserAvailable(user);
        return buildTokenResp(user);
    }

    @Override
    /**
     * 刷新 token。
     *
     * <p>只接受 refresh token，拒绝 access token 误用；解析出用户后重新查询数据库状态和角色。</p>
     */
    public TokenResp refresh(RefreshTokenReq req) {

        Claims claims = jwtUtil.parseToken(req.getRefreshToken());
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        if (claims.getId() == null || claims.getId().isBlank() || tokenRevocationService.isRevoked(claims.getId())) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = jwtUtil.getUserId(claims);
        User user = userService.getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        userService.checkUserAvailable(user);
        jwtUtil.validateAuthVersion(claims, user);
        return buildTokenResp(user);
    }

    @Override
    public void logout(String accessToken, RefreshTokenReq req) {
        Claims accessClaims = jwtUtil.parseAccessToken(accessToken);
        if (tokenRevocationService.isRevoked(accessClaims.getId())) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Claims refreshClaims = jwtUtil.parseToken(req.getRefreshToken());
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(refreshClaims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))
                || !accessClaims.getSubject().equals(refreshClaims.getSubject())
                || refreshClaims.getId() == null || refreshClaims.getId().isBlank()
                || tokenRevocationService.isRevoked(refreshClaims.getId())) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        tokenRevocationService.revoke(accessClaims);
        tokenRevocationService.revoke(refreshClaims);
    }

    private User findEmail(String email) {
        return userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email).last("LIMIT 1"));
    }

    @Override
    public void sendEmailCode(EmailAuthReq.SendCode req, String ip) {
        if (req.getPurpose() == Purpose.MIGRATION) {
            throw new BizException(ResultCode.BAD_REQUEST, "请使用旧账号迁移入口");
        }
        String email = EmailCodeManager.normalize(req.getEmail());
        User user = findEmail(email);
        boolean eligible = req.getPurpose() == Purpose.REGISTER
                ? userMapper.countEmailIncludingDeleted(email) == 0
                : user != null && user.getEmailVerifiedAt() != null && Integer.valueOf(1).equals(user.getStatus());
        emailCodes.send(email, req.getPurpose(), "", ip, eligible);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(EmailAuthReq.ResetPassword req) {
        String email = EmailCodeManager.normalize(req.getEmail());
        emailCodes.consume(email, Purpose.RESET_PASSWORD, "", req.getCode());
        User user = findEmail(email);
        if (user == null || user.getEmailVerifiedAt() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证码无效或已过期");
        }
        userService.checkUserAvailable(user);
        if (userMapper.resetPassword(user.getId(), user.getAuthVersion(), passwordEncoder.encode(req.getNewPassword())) != 1) {
            throw new BizException(ResultCode.CONFLICT, "账号已发生变化，请重新操作");
        }
    }

    @Override
    public EmailAuthReq.MigrationToken verifyMigration(EmailAuthReq.VerifyMigration req, String ip) {
        // Limit credential guessing independently from mail sending.
        String key = "auth:migration-attempt:" + ip;
        Long attempts = redis.execute(MIGRATION_ATTEMPT, List.of(key));
        if (attempts == null || attempts > 20) {
            throw new BizException(ResultCode.BAD_REQUEST, "操作过于频繁，请稍后重试");
        }
        User user = userService.getByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        userService.checkUserAvailable(user);
        if (user.getEmailVerifiedAt() != null) {
            throw new BizException(ResultCode.CONFLICT, "账号已绑定邮箱，请使用邮箱登录");
        }
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set("auth:migration:" + token, user.getId() + ":" + user.getAuthVersion(), Duration.ofMinutes(10));
        return new EmailAuthReq.MigrationToken(token, 600);
    }

    private User migrationUser(String token) {
        String value = redis.opsForValue().get("auth:migration:" + token);
        if (value == null) { throw new BizException(ResultCode.BAD_REQUEST, "迁移凭证已过期，请重新验证原账号"); }
        String[] parts = value.split(":");
        User user = userService.getById(Long.valueOf(parts[0]));
        if (user == null || user.getEmailVerifiedAt() != null || !String.valueOf(user.getAuthVersion()).equals(parts[1])) {
            throw new BizException(ResultCode.CONFLICT, "账号已发生变化，请重新验证");
        }
        userService.checkUserAvailable(user);
        return user;
    }

    @Override
    public void sendMigrationCode(EmailAuthReq.MigrationEmail req, String ip) {
        User user = migrationUser(req.getMigrationToken());
        String email = EmailCodeManager.normalize(req.getEmail());
        boolean eligible = email.equals(user.getEmail()) || userMapper.countEmailIncludingDeleted(email) == 0;
        emailCodes.send(email, Purpose.MIGRATION, req.getMigrationToken(), ip, eligible);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenResp bindMigration(EmailAuthReq.BindMigration req) {
        User user = migrationUser(req.getMigrationToken());
        String email = EmailCodeManager.normalize(req.getEmail());
        emailCodes.consume(email, Purpose.MIGRATION, req.getMigrationToken(), req.getCode());
        if (!email.equals(user.getEmail()) && userMapper.countEmailIncludingDeleted(email) > 0) {
            throw new BizException(ResultCode.CONFLICT, "邮箱无法绑定");
        }
        try {
            if (userMapper.bindEmail(user.getId(), user.getAuthVersion(), email) != 1) {
                throw new BizException(ResultCode.CONFLICT, "账号已发生变化，请重新验证");
            }
        } catch (DuplicateKeyException exception) {
            throw new BizException(ResultCode.CONFLICT, "邮箱无法绑定");
        }
        redis.delete("auth:migration:" + req.getMigrationToken());
        return buildTokenResp(userService.getById(user.getId()));
    }

    /**
     * 组装 token 响应。
     *
     * <p>角色编码会写入 token，同时响应体里也带用户资料，方便前端登录后一次性初始化用户态。</p>
     */
    private TokenResp buildTokenResp(User user) {
        List<String> roles = userService.listRoleCodes(user.getId());
        TokenResp resp = new TokenResp();
        resp.setAccessToken(jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles, user.getAuthVersion()));
        resp.setRefreshToken(jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roles, user.getAuthVersion()));
        resp.setExpiresIn(jwtProperties.getAccessExpireMs() / 1000);
        resp.setUser(userService.toUserResp(user));
        return resp;
    }
}
