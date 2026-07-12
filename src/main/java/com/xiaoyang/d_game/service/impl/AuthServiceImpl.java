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
import com.xiaoyang.d_game.service.AuthService;
import com.xiaoyang.d_game.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_USER = "USER";

    private final UserService userService;
    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper sysRoleMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenResp register(RegisterReq req) {
        log.debug("开始注册用户, username={}", req.getUsername());
        if (userService.getByUsername(req.getUsername()) != null) {
            throw new BizException(ResultCode.USER_EXISTS);
        }
        if (StringUtils.hasText(req.getEmail())) {
            User emailUser = userService.getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, req.getEmail())
                    .last("LIMIT 1"));
            if (emailUser != null) {
                throw new BizException(ResultCode.CONFLICT, "邮箱已被使用");
            }
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : req.getUsername());
        user.setEmail(req.getEmail());
        user.setMobile(req.getMobile());
        userService.save(user);

        SysRole userRole = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, ROLE_USER)
                .last("LIMIT 1"));
        if (userRole != null) {
            UserRoleRel rel = new UserRoleRel();
            rel.setUserId(user.getId());
            rel.setRoleId(userRole.getId());
            userRoleRelMapper.insert(rel);
        }
        log.debug("用户注册成功, userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResp(user);
    }

    @Override
    public TokenResp login(LoginReq req) {
        log.debug("开始登录校验, username={}", req.getUsername());
        User user = userService.getByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        userService.checkUserAvailable(user);
        log.debug("用户登录成功, userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResp(user);
    }

    @Override
    public TokenResp refresh(RefreshTokenReq req) {
        log.debug("开始刷新 Token");
        Claims claims = jwtUtil.parseToken(req.getRefreshToken());
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = userService.getById(userId);
        if (user == null) {
            log.debug("刷新 Token 失败, 用户不存在, userId={}", userId);
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        userService.checkUserAvailable(user);
        log.debug("刷新 Token 成功, userId={}, username={}", user.getId(), user.getUsername());
        return buildTokenResp(user);
    }

    private TokenResp buildTokenResp(User user) {
        List<String> roles = userService.listRoleCodes(user.getId());
        TokenResp resp = new TokenResp();
        resp.setAccessToken(jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles));
        resp.setRefreshToken(jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roles));
        resp.setExpiresIn(jwtProperties.getAccessExpireMs() / 1000);
        resp.setUser(userService.toUserResp(user));
        return resp;
    }
}
