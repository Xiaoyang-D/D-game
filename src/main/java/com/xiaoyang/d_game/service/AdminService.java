package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.AssignRoleReq;
import com.xiaoyang.d_game.dto.AuditLogQueryReq;
import com.xiaoyang.d_game.dto.AuditLogResp;
import com.xiaoyang.d_game.dto.AuditReq;
import com.xiaoyang.d_game.dto.BatchAuditReq;
import com.xiaoyang.d_game.entity.SysRole;

import java.util.List;

public interface AdminService {

    void banUser(Long userId);

    void unbanUser(Long userId);

    void auditPost(Long postId, AuditReq req);

    void batchAuditPosts(BatchAuditReq req);

    void deletePost(Long postId);

    void auditComment(Long commentId, AuditReq req);

    List<SysRole> listRoles();

    void assignRole(AssignRoleReq req);

    PageResult<AuditLogResp> pageAuditLogs(AuditLogQueryReq req);
}
