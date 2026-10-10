package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.ContentStatusEnum;
import com.xiaoyang.d_game.common.enums.NotificationTypeEnum;
import com.xiaoyang.d_game.common.enums.TargetTypeEnum;
import com.xiaoyang.d_game.common.enums.UserStatusEnum;
import com.xiaoyang.d_game.dto.AssignRoleReq;
import com.xiaoyang.d_game.dto.AuditLogQueryReq;
import com.xiaoyang.d_game.dto.AuditLogResp;
import com.xiaoyang.d_game.dto.AuditReq;
import com.xiaoyang.d_game.dto.BatchAuditReq;
import com.xiaoyang.d_game.entity.AuditLog;
import com.xiaoyang.d_game.entity.Comment;
import com.xiaoyang.d_game.entity.Post;
import com.xiaoyang.d_game.entity.SysRole;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserRoleRel;
import com.xiaoyang.d_game.mapper.AuditLogMapper;
import com.xiaoyang.d_game.mapper.CommentMapper;
import com.xiaoyang.d_game.mapper.PostMapper;
import com.xiaoyang.d_game.mapper.SysRoleMapper;
import com.xiaoyang.d_game.mapper.UserMapper;
import com.xiaoyang.d_game.mapper.UserRoleRelMapper;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.AdminService;
import com.xiaoyang.d_game.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * 后台管理业务实现。
 *
 * <p>管理员动作通常会改变用户或内容状态，因此大多数写操作都带事务，并写入审计日志。
 * 内容审核还会通过站内通知把结果反馈给作者。</p>
 */
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final AuditLogMapper auditLogMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 封禁用户。
     */
    public void banUser(Long userId) {
        requireUser(userId);
        User user = new User();
        user.setId(userId);
        user.setNickname(null);
        user.setBio(null);
        user.setStatus(UserStatusEnum.BANNED.getCode());
        userMapper.updateById(user);
        writeAuditLog("BAN_USER", "USER", userId, "封禁用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 解封用户。
     */
    public void unbanUser(Long userId) {
        requireUser(userId);
        User user = new User();
        user.setId(userId);
        user.setNickname(null);
        user.setBio(null);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        userMapper.updateById(user);
        writeAuditLog("UNBAN_USER", "USER", userId, "解封用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 审核帖子。
     *
     * <p>帖子状态、审计日志和通知发送在同一事务里完成；通知失败如果抛出运行时异常也会回滚审核结果。</p>
     */
    public void auditPost(Long postId, AuditReq req) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        if (Objects.equals(post.getStatus(), ContentStatusEnum.DRAFT.getCode())) {
            throw new BizException(ResultCode.BAD_REQUEST, "草稿不能封禁或公开，请等待作者发布");
        }
        // 保留原接口兼容：approved=false 封禁，true 解封/通过历史待审核内容。
        int status = req.getApproved() ? ContentStatusEnum.APPROVED.getCode() : ContentStatusEnum.REJECTED.getCode();
        if (Objects.equals(post.getStatus(), status)) {
            return;
        }
        int changed = postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId).eq(Post::getStatus, post.getStatus()).set(Post::getStatus, status));
        if (changed == 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "帖子状态已变化，请刷新后重试");
        }
        String reason = req.getReason() == null ? "" : req.getReason().trim();
        writeAuditLog(req.getApproved() ? "UNBAN_POST" : "BAN_POST", "POST", postId,
                (req.getApproved() ? "解封帖子" : "封禁帖子") + (reason.isEmpty() ? "" : ": " + reason));
        notificationService.sendNotification(post.getUserId(), CurrentUser.getUserId(),
                NotificationTypeEnum.AUDIT.getCode(), "帖子管理通知",
                (req.getApproved() ? "你的帖子已恢复公开" : "你的帖子已被管理员封禁")
                        + (reason.isEmpty() ? "" : "，原因：" + reason),
                TargetTypeEnum.POST.getCode(), postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 管理员删除帖子。
     */
    public void deletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        postMapper.deleteById(postId);
        writeAuditLog("DELETE_POST", "POST", postId, "删除帖子: " + post.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 批量审核帖子。
     *
     * <p>为了避免某个非待审核帖子影响整批处理，遇到非待审核状态直接跳过；非法 ID 或不存在帖子仍会抛错。</p>
     */
    public void batchAuditPosts(BatchAuditReq req) {
        for (String postIdStr : req.getPostIds()) {
            Long postId;
            try {
                postId = Long.parseLong(postIdStr);
            } catch (NumberFormatException ex) {
                throw new BizException(ResultCode.BAD_REQUEST, "无效的帖子 ID: " + postIdStr);
            }
            Post post = postMapper.selectById(postId);
            if (post == null) {
                throw new BizException(ResultCode.POST_NOT_FOUND, "帖子不存在: " + postIdStr);
            }
            if (!Objects.equals(post.getStatus(), ContentStatusEnum.PENDING.getCode())) {
                // 只处理待审核内容，已经审核过的帖子保持原状态。
                continue;
            }
            AuditReq auditReq = new AuditReq();
            auditReq.setApproved(req.getApproved());
            auditReq.setReason(req.getReason());
            auditPost(postId, auditReq);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 审核评论。
     */
    public void auditComment(Long commentId, AuditReq req) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }
        int status = req.getApproved() ? ContentStatusEnum.APPROVED.getCode() : ContentStatusEnum.REJECTED.getCode();
        comment.setStatus(status);
        commentMapper.updateById(comment);
        writeAuditLog("AUDIT_COMMENT", "COMMENT", commentId, req.getApproved() ? "审核通过" : "审核拒绝");
    }

    @Override
    /**
     * 查询角色字典。
     */
    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 给用户分配角色。
     *
     * <p>先检查是否已有同样关系，避免重复插入触发唯一键冲突。</p>
     */
    public void assignRole(AssignRoleReq req) {
        requireUser(req.getUserId());
        SysRole role = sysRoleMapper.selectById(req.getRoleId());
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在");
        }
        UserRoleRel existing = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRel>()
                .eq(UserRoleRel::getUserId, req.getUserId())
                .eq(UserRoleRel::getRoleId, req.getRoleId())
                .last("LIMIT 1"));
        if (existing != null) {
            // 幂等处理：已经拥有该角色时直接成功返回。
            return;
        }
        UserRoleRel rel = new UserRoleRel();
        rel.setUserId(req.getUserId());
        rel.setRoleId(req.getRoleId());
        userRoleRelMapper.insert(rel);
        writeAuditLog("ASSIGN_ROLE", "USER", req.getUserId(), "分配角色:" + role.getRoleCode());
    }

    @Override
    /**
     * 分页查询审计日志。
     *
     * <p>先分页查询审计日志，再批量加载操作人昵称，避免逐条查询用户信息。</p>
     */
    public PageResult<AuditLogResp> pageAuditLogs(AuditLogQueryReq req) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (req.getOperatorId() != null) {
            wrapper.eq(AuditLog::getOperatorId, req.getOperatorId());
        }
        if (req.getAction() != null && !req.getAction().isBlank()) {
            wrapper.eq(AuditLog::getAction, req.getAction());
        }
        if (req.getTargetType() != null && !req.getTargetType().isBlank()) {
            wrapper.eq(AuditLog::getTargetType, req.getTargetType());
        }
        if (req.getTargetId() != null) {
            wrapper.eq(AuditLog::getTargetId, req.getTargetId());
        }
        if (req.getStartTime() != null) {
            wrapper.ge(AuditLog::getGmtCreate, req.getStartTime());
        }
        if (req.getEndTime() != null) {
            wrapper.le(AuditLog::getGmtCreate, req.getEndTime());
        }
        wrapper.orderByDesc(AuditLog::getGmtCreate);

        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(req.getPage(), req.getSize()), wrapper);
        List<Long> operatorIds = page.getRecords().stream()
                .map(AuditLog::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> operatorMap = operatorIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(operatorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<AuditLogResp> records = page.getRecords().stream()
                .map(log -> toAuditLogResp(log, operatorMap.get(log.getOperatorId())))
                .toList();

        PageResult<AuditLogResp> result = new PageResult<>();
        result.setPage(page.getCurrent());
        result.setSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setRecords(records);
        return result;
    }

    /**
     * 查询用户，不存在则抛业务异常。
     */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 写入管理员操作审计日志。
     *
     * <p>operatorId 来自当前登录上下文，因此后台接口必须经过 ADMIN 鉴权。</p>
     */
    private void writeAuditLog(String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(CurrentUser.getUserId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    /**
     * 将审计日志实体转换成响应对象，并补充操作人昵称。
     */
    private AuditLogResp toAuditLogResp(AuditLog log, User operator) {
        AuditLogResp resp = new AuditLogResp();
        resp.setId(log.getId());
        resp.setOperatorId(log.getOperatorId());
        resp.setOperatorNickname(operator == null ? null : operator.getNickname());
        resp.setAction(log.getAction());
        resp.setTargetType(log.getTargetType());
        resp.setTargetId(log.getTargetId());
        resp.setDetail(log.getDetail());
        resp.setGmtCreate(log.getGmtCreate());
        return resp;
    }
}
