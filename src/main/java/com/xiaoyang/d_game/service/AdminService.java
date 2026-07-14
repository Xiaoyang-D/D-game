package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.AssignRoleReq;
import com.xiaoyang.d_game.dto.AuditLogQueryReq;
import com.xiaoyang.d_game.dto.AuditLogResp;
import com.xiaoyang.d_game.dto.AuditReq;
import com.xiaoyang.d_game.dto.BatchAuditReq;
import com.xiaoyang.d_game.entity.SysRole;

import java.util.List;

/**
 * 后台管理业务接口。
 *
 * <p>封装管理员操作的核心能力，包括用户状态管理、内容审核、角色分配和审计日志查询。
 * 实现类会负责写审计日志、发送必要通知和保证多表状态的一致性。</p>
 */
public interface AdminService {

    /**
     * 封禁用户。
     *
     * @param userId 被封禁用户 ID
     */
    void banUser(Long userId);

    /**
     * 解除用户封禁。
     *
     * @param userId 被解封用户 ID
     */
    void unbanUser(Long userId);

    /**
     * 审核单个帖子，并通知作者审核结果。
     */
    void auditPost(Long postId, AuditReq req);

    /**
     * 批量审核帖子。
     *
     * <p>只处理待审核状态的帖子，其他状态会跳过，避免重复审核导致状态来回跳变。</p>
     */
    void batchAuditPosts(BatchAuditReq req);

    /**
     * 删除帖子并记录审计日志。
     */
    void deletePost(Long postId);

    /**
     * 审核单个评论。
     */
    void auditComment(Long commentId, AuditReq req);

    /**
     * 查询系统角色列表。
     */
    List<SysRole> listRoles();

    /**
     * 给用户分配角色。
     */
    void assignRole(AssignRoleReq req);

    /**
     * 分页查询审计日志。
     */
    PageResult<AuditLogResp> pageAuditLogs(AuditLogQueryReq req);
}
