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

/**
 * 后台管理接口。
 *
 * <p>整个 Controller 都要求 ADMIN 角色，主要覆盖内容审核、用户封禁、角色分配和审计日志查询。
 * Controller 只做路由和参数绑定，真正的状态流转、通知发送和审计日志写入由 {@link AdminService} 完成。</p>
 */
@Tag(name = "后台管理")
@RequireRole("ADMIN")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PostService postService;

    /**
     * 查询所有待审核帖子。
     *
     * <p>后台审核台使用该接口拉取状态为“待审核”的帖子，按创建时间升序展示，方便先提交的内容先处理。</p>
     */
    @Operation(summary = "待审核帖子列表")
    @GetMapping("/posts/pending")
    public Result<PageResult<PostResp>> pendingPosts(@RequestParam(defaultValue = "1") Long page,
                                                     @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pagePendingPosts(page, size));
    }

    /**
     * 批量审核帖子。
     *
     * <p>请求体中包含多个帖子 ID 和统一的审核结果，服务层会逐条跳过非待审核帖子，并给被审核用户发送通知。</p>
     */
    @Operation(summary = "批量审核帖子")
    @PostMapping("/posts/batch-audit")
    public Result<Void> batchAuditPosts(@Valid @RequestBody BatchAuditReq req) {
        adminService.batchAuditPosts(req);
        return Result.success();
    }

    /**
     * 查询封禁作者发布过的帖子。
     *
     * <p>用于管理员复查封禁用户的历史内容，可按作者 ID 或昵称/用户名关键字筛选。</p>
     */
    @Operation(summary = "封禁作者的帖子列表")
    @GetMapping("/posts/banned-authors")
    public Result<PageResult<PostResp>> bannedAuthorPosts(BannedAuthorPostQueryReq req) {
        return Result.success(postService.pagePostsByBannedAuthors(req));
    }

    /**
     * 管理员删除帖子。
     *
     * <p>删除动作走 MyBatis-Plus 逻辑删除，并写入审计日志，方便后台追踪操作人和被删内容。</p>
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{postId}")
    public Result<Void> deletePost(@PathVariable String postId) {
        adminService.deletePost(Long.parseLong(postId));
        return Result.success();
    }

    /**
     * 分页查询后台审计日志。
     *
     * <p>支持按操作人、动作、目标类型、目标 ID 和时间范围过滤，用于管理操作追溯。</p>
     */
    @Operation(summary = "审计日志列表")
    @GetMapping("/audit-logs")
    public Result<PageResult<AuditLogResp>> auditLogs(AuditLogQueryReq req) {
        return Result.success(adminService.pageAuditLogs(req));
    }

    /**
     * 封禁用户账号。
     *
     * <p>封禁后用户仍存在，但登录态会在受保护接口中被拦截，无法继续发帖、评论或互动。</p>
     */
    @Operation(summary = "封禁用户")
    @PutMapping("/users/{userId}/ban")
    public Result<Void> banUser(@PathVariable Long userId) {
        adminService.banUser(userId);
        return Result.success();
    }

    /**
     * 解除用户封禁。
     *
     * <p>把用户状态恢复为正常，后续鉴权会重新允许其访问需要登录的接口。</p>
     */
    @Operation(summary = "解封用户")
    @PutMapping("/users/{userId}/unban")
    public Result<Void> unbanUser(@PathVariable Long userId) {
        adminService.unbanUser(userId);
        return Result.success();
    }

    /**
     * 审核单个帖子。
     *
     * <p>审核结果会写回帖子状态、记录审计日志，并通过站内通知告知作者。</p>
     */
    @Operation(summary = "审核帖子")
    @PostMapping("/posts/{postId}/audit")
    public Result<Void> auditPost(@PathVariable Long postId, @Valid @RequestBody AuditReq req) {
        adminService.auditPost(postId, req);
        return Result.success();
    }

    /**
     * 审核单个评论。
     *
     * <p>评论审核目前只维护状态和审计日志，不额外发送通知。</p>
     */
    @Operation(summary = "审核评论")
    @PostMapping("/comments/{commentId}/audit")
    public Result<Void> auditComment(@PathVariable Long commentId, @Valid @RequestBody AuditReq req) {
        adminService.auditComment(commentId, req);
        return Result.success();
    }

    /**
     * 查询系统角色列表。
     *
     * <p>用于后台分配角色时展示可选项，角色编码和角色名称来自 {@code sys_role} 表。</p>
     */
    @Operation(summary = "角色列表")
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(adminService.listRoles());
    }

    /**
     * 给用户分配角色。
     *
     * <p>服务层会校验用户和角色存在，并避免重复插入同一个用户-角色关系。</p>
     */
    @Operation(summary = "分配角色")
    @PostMapping("/roles/assign")
    public Result<Void> assignRole(@Valid @RequestBody AssignRoleReq req) {
        adminService.assignRole(req);
        return Result.success();
    }
}
