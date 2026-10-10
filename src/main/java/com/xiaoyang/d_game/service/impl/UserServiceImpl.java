package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.HtmlSanitizer;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.UserFavoriteResp;
import com.xiaoyang.d_game.dto.UserProfileResp;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.SysRole;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserFavorite;
import com.xiaoyang.d_game.entity.UserFollow;
import com.xiaoyang.d_game.entity.UserRoleRel;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.SysRoleMapper;
import com.xiaoyang.d_game.mapper.UserFavoriteMapper;
import com.xiaoyang.d_game.mapper.UserFollowMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.mapper.UserRoleRelMapper;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户业务实现。
 * <p>负责当前用户资料、公开用户主页统计和公开收藏流，并统一处理封禁用户的可见性规则。</p>
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserRoleRelMapper userRoleRelMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PostMapper postMapper;
    private final UserFollowMapper userFollowMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final HtmlSanitizer htmlSanitizer;

    @Override
    /**
     * 根据用户名查询用户。
     */
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    /**
     * 查询用户，不存在时抛出业务异常。
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
     * 查询用户关联的角色编码。
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
     * 将用户实体转换为包含敏感字段的当前用户资料响应。
     */
    public UserResp toUserResp(User user) {
        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setEmailVerifiedAt(user.getEmailVerifiedAt());
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
     * 获取当前登录用户资料。
     */
    public UserResp getCurrentUserProfile() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return toUserResp(getRequiredUser(userId));
    }

    @Override
    /**
     * 获取公开用户主页资料和统计数据。
     *
     * <p>普通访客不会看到邮箱、手机号等敏感字段；封禁用户仅允许本人或管理员查看。</p>
     */
    public UserProfileResp getPublicProfile(Long userId) {
        User user = getRequiredUser(userId);
        Long currentUserId = CurrentUser.getOptionalUserId();
        boolean isSelf = currentUserId != null && Objects.equals(currentUserId, userId);
        boolean isAdmin = isCurrentUserAdmin();
        if (Objects.equals(user.getStatus(), UserStatusEnum.BANNED.getCode()) && !isSelf && !isAdmin) {
            throw new BizException(ResultCode.USER_BANNED);
        }

        UserProfileResp resp = new UserProfileResp();
        resp.setId(user.getId());
        resp.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : "未设置昵称");
        resp.setAvatarUrl(user.getAvatarUrl());
        resp.setBio(StringUtils.hasText(user.getBio()) ? user.getBio() : "");
        resp.setGmtCreate(user.getGmtCreate());
        resp.setPostCount(postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, ContentStatusEnum.APPROVED.getCode())));
        Long likeCount = postMapper.sumPublishedLikeCountByUser(userId);
        resp.setLikeCount(likeCount == null ? 0L : likeCount);
        resp.setFollowingCount(userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId)));
        resp.setFollowerCount(userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, userId)));
        Long favoriteCount = userFavoriteMapper.countVisibleFavorites(userId);
        resp.setFavoriteCount(favoriteCount == null ? 0L : favoriteCount);
        resp.setSelf(isSelf);
        resp.setFollowing(isFollowing(currentUserId, userId, isSelf));
        return resp;
    }

    @Override
    /**
     * 分页查询用户公开可见的收藏流。
     */
    public PageResult<UserFavoriteResp> pagePublicFavorites(Long userId, Long page, Long size) {
        User user = getRequiredUser(userId);
        Long currentUserId = CurrentUser.getOptionalUserId();
        boolean isSelf = currentUserId != null && Objects.equals(currentUserId, userId);
        boolean isAdmin = isCurrentUserAdmin();
        if (Objects.equals(user.getStatus(), UserStatusEnum.BANNED.getCode()) && !isSelf && !isAdmin) {
            throw new BizException(ResultCode.USER_BANNED);
        }

        long safePage = page == null || page < 1 ? 1L : page;
        long safeSize = size == null || size < 1 ? 10L : size;
        long offset = (safePage - 1) * safeSize;
        List<UserFavoriteResp> records = userFavoriteMapper.selectVisibleFavorites(userId, offset, safeSize);
        records.forEach(record -> record.setContent(htmlSanitizer.sanitizePostContent(record.getContent())));
        Long total = userFavoriteMapper.countVisibleFavorites(userId);

        PageResult<UserFavoriteResp> result = new PageResult<>();
        result.setPage(safePage);
        result.setSize(safeSize);
        result.setTotal(total == null ? 0L : total);
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 更新当前登录用户资料，并校验邮箱和手机号唯一性。
     */
    public UserResp updateProfile(UpdateProfileReq req) {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        getRequiredUser(userId);
        User user = new User();
        user.setId(userId);
        user.setNickname(null);
        user.setBio(null);
        user.setStatus(null);
        if (StringUtils.hasText(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (req.getEmail() != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "邮箱必须通过专用验证流程绑定，暂不支持更换");
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
        return toUserResp(getRequiredUser(userId));
    }

    /**
     * 判断当前访问者是否具有管理员角色。
     */
    private boolean isCurrentUserAdmin() {
        return CurrentUser.hasRole("ADMIN");
    }

    /**
     * 判断当前访问者是否已关注目标用户。
     */
    private boolean isFollowing(Long currentUserId, Long targetUserId, boolean isSelf) {
        if (currentUserId == null || isSelf) {
            return false;
        }
        return userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, targetUserId)) > 0;
    }

    /**
     * 校验手机号唯一性。
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
     * 检查用户是否处于可用状态。
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
