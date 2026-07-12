package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.xiaoyang.d_game.security.UserContext;
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
    public void banUser(Long userId) {
        User user = requireUser(userId);
        user.setStatus(UserStatusEnum.BANNED.getCode());
        userMapper.updateById(user);
        writeAuditLog("BAN_USER", "USER", userId, "封禁用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        User user = requireUser(userId);
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        userMapper.updateById(user);
        writeAuditLog("UNBAN_USER", "USER", userId, "解封用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPost(Long postId, AuditReq req) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_FOUND);
        }
        int status = req.getApproved() ? ContentStatusEnum.APPROVED.getCode() : ContentStatusEnum.REJECTED.getCode();
        post.setStatus(status);
        postMapper.updateById(post);
        writeAuditLog("AUDIT_POST", "POST", postId, req.getApproved() ? "审核通过" : "审核拒绝");
        notificationService.sendNotification(post.getUserId(), UserContext.getUserId(),
                NotificationTypeEnum.AUDIT.getCode(), "帖子审核结果",
                req.getApproved() ? "你的帖子已通过审核" : "你的帖子未通过审核",
                TargetTypeEnum.POST.getCode(), postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            return;
        }
        UserRoleRel rel = new UserRoleRel();
        rel.setUserId(req.getUserId());
        rel.setRoleId(req.getRoleId());
        userRoleRelMapper.insert(rel);
        writeAuditLog("ASSIGN_ROLE", "USER", req.getUserId(), "分配角色:" + role.getRoleCode());
    }

    @Override
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

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void writeAuditLog(String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(UserContext.getUserId());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

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
