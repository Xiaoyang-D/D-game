package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.entity.Notification;
import com.xiaoyang.d_game.dto.NotificationResp;
import com.xiaoyang.d_game.dto.MessageDtos;
import org.springframework.web.bind.annotation.RequestBody;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内通知接口。
 *
 * <p>通知由点赞、评论、关注、审核等业务动作触发。用户只能查看和修改自己的通知，
 * 当前用户 ID 由登录上下文提供，Controller 不接收用户 ID 参数。</p>
 */
@Tag(name = "通知")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 分页查询当前用户通知。
     *
     * <p>按创建时间倒序返回，最新通知排在前面。</p>
     */
    @RequireLogin
    @Operation(summary = "通知分页列表")
    @GetMapping
    public Result<PageResult<NotificationResp>> list(@RequestParam(defaultValue = "1") Long page,
                                                 @RequestParam(defaultValue = "10") Long size,
                                                 @RequestParam(required=false) String category) {
        return Result.success(notificationService.pageNotifications(page, size, category));
    }

    /**
     * 查询当前用户未读通知数。
     *
     * <p>前端可用于导航栏红点或消息中心徽标。</p>
     */
    @RequireLogin
    @Operation(summary = "未读数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    /**
     * 标记单条通知为已读。
     */
    @RequireLogin
    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    /**
     * 将当前用户所有未读通知标记为已读。
     */
    @RequireLogin
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestParam(required=false) String category) {
        notificationService.markAllRead(category);
        return Result.success();
    }
    @RequireLogin @GetMapping("/settings")
    public Result<MessageDtos.Settings> settings() { return Result.success(notificationService.settings()); }
    @RequireLogin @PutMapping("/settings")
    public Result<Void> settings(@RequestBody MessageDtos.Settings settings) {
        notificationService.saveSettings(settings); return Result.success();
    }
}
