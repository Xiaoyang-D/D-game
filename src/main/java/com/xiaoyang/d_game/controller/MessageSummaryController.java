package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 导航栏和消息中心共用数据库未读汇总，避免各页面自行累加。 */
@RestController
@RequiredArgsConstructor
public class MessageSummaryController {
    private final ChatService chat;
    private final NotificationService notifications;

    @RequireLogin
    @GetMapping("/api/v1/messages/unread-summary")
    public Result<MessageDtos.Unread> unread() {
        long privateMessages = chat.unread(), replies = notifications.categoryUnread("reply"), mentions = notifications.categoryUnread("mention"),
                likes = notifications.categoryUnread("like"), system = notifications.categoryUnread("system"), social = notifications.categoryUnread("social");
        return Result.success(new MessageDtos.Unread(privateMessages + replies + mentions + likes + system + social, privateMessages, replies, mentions, likes, system, social));
    }
}
