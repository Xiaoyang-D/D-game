package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.AssignRoleReq;
import com.xiaoyang.d_game.dto.AuditLogQueryReq;
import com.xiaoyang.d_game.dto.AuditLogResp;
import com.xiaoyang.d_game.dto.AuditReq;
import com.xiaoyang.d_game.dto.BatchAuditReq;
import com.xiaoyang.d_game.dto.BannedAuthorPostQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.entity.SysRole;
import com.xiaoyang.d_game.security.RequireRole;
import com.xiaoyang.d_game.service.AdminService;
import com.xiaoyang.d_game.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "后台管理")
@RequireRole("ADMIN")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PostService postService;

    @Operation(summary = "待审核帖子列表")
    @GetMapping("/posts/pending")
    public Result<PageResult<PostResp>> pendingPosts(@RequestParam(defaultValue = "1") Long page,
                                                     @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pagePendingPosts(page, size));
    }

    @Operation(summary = "批量审核帖子")
    @PostMapping("/posts/batch-audit")
    public Result<Void> batchAuditPosts(@Valid @RequestBody BatchAuditReq req) {
        adminService.batchAuditPosts(req);
        return Result.success();
    }

    @Operation(summary = "封禁作者的帖子列表")
    @GetMapping("/posts/banned-authors")
    public Result<PageResult<PostResp>> bannedAuthorPosts(BannedAuthorPostQueryReq req) {
        return Result.success(postService.pagePostsByBannedAuthors(req));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(@PathVariable String postId) {
        adminService.deletePost(Long.parseLong(postId));
        return Result.success();
    }

    @Operation(summary = "审计日志列表")
    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLogResp>> auditLogs(AuditLogQueryReq req) {
        return Result.success(adminService.pageAuditLogs(req));
    }

    @Operation(summary = "封禁用户")
    @PutMapping("/users/{userId}/ban")
    public Result<Void> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return Result.success();
    }

    @Operation(summary = "解封用户")
    @PutMapping("/users/{userId}/unban")
    public Result<Void> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return Result.success();
    }

    @Operation(summary = "审核帖子")
    @PostMapping("/posts/{postId}/audit")
    public Result<Void> auditPost(@PathVariable Long postId, @Valid @RequestBody AuditReq req) {
        adminService.auditPost(postId, req);
        return Result.success();
    }

    @Operation(summary = "审核评论")
    @PostMapping("/comments/{commentId}/audit")
    public Result<Void> auditComment(@PathVariable Long commentId, @Valid @RequestBody AuditReq req) {
        adminService.auditComment(commentId, req);
        return Result.success();
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(adminService.listRoles());
    }

    @Operation(summary = "分配角色")
    @PostMapping("/roles/assign")
    public Result<Void> assignRole(@Valid @RequestBody AssignRoleReq req) {
        adminService.assignRole(req);
        return Result.success();
    }
}
