package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.entity.SysRole;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserRoleRel;
import com.xiaoyang.d_game.mapper.SysRoleMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.mapper.UserRoleRelMapper;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 用户业务实现。
 *
 * <p>负责用户基础查询、角色聚合、资料响应转换和个人资料更新。
 * 敏感字段如密码哈希不会进入响应 DTO。</p>
 */
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    /**
     * 按用户名查询用户。
     */
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    /**
     * 查询必需用户。
     */
    public User getRequiredUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    /**
     * 查询用户角色编码。
     *
     * <p>先查用户-角色关系，再批量查角色表，避免角色表和关系表强耦合在调用方。</p>
     */
    public List<String> listRoleCodes(Long userId) {
        List<UserRoleRel> relations = userRoleRelMapper.selectList(new LambdaQueryWrapper<UserRoleRel>()
                .eq(UserRoleRel::getUserId, userId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(UserRoleRel::getRoleId).toList();
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    /**
     * 转换用户响应。
     */
    public UserResp toUserResp(User user) {
        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setMobile(user.getMobile());
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setBio(user.getBio());
        resp.setStatus(user.getStatus());
        resp.setRoles(listRoleCodes(user.getId()));
        resp.setGmtCreate(user.getGmtCreate());
        return resp;
    }

    @Override
    /**
     * 获取当前用户资料。
     */
    public UserResp getCurrentUserProfile() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return toUserResp(getRequiredUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新当前用户资料。
     *
     * <p>可选字段采用“传了才更新”的策略；邮箱和手机号更新前会校验唯一性。</p>
     */
    public UserResp updateProfile(UpdateProfileReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        User user = getRequiredUser(userId);
        if (StringUtils.hasText(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (StringUtils.hasText(req.getEmail())) {
            // 邮箱/手机号是唯一字段，排除当前用户后查重。
            checkEmailUnique(req.getEmail(), userId);
            user.setEmail(req.getEmail());
        }
        if (StringUtils.hasText(req.getMobile())) {
            checkMobileUnique(req.getMobile(), userId);
            user.setMobile(req.getMobile());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }
        updateById(user);
        return toUserResp(user);
    }

    /**
     * 校验邮箱未被其他用户使用。
     */
    private void checkEmailUnique(String email, Long userId) {
        User exists = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getId, userId)
                .last("LIMIT 1"));
        if (exists != null) {
            throw new BizException(ResultCode.CONFLICT, "邮箱已被使用");
        }
    }

    /**
     * 校验手机号未被其他用户使用。
     */
    private void checkMobileUnique(String mobile, Long userId) {
        User exists = getOne(new LambdaQueryWrapper<User>()
                .eq(User::getMobile, mobile)
                .ne(User::getId, userId)
                .last("LIMIT 1"));
        if (exists != null) {
            throw new BizException(ResultCode.CONFLICT, "手机号已被使用");
        }
    }

    @Override
    /**
     * 校验用户状态。
     */
    public void checkUserAvailable(User user) {
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (Objects.equals(user.getStatus(), UserStatusEnum.BANNED.getCode())) {
            throw new BizException(ResultCode.USER_BANNED);
        }
    }
}
