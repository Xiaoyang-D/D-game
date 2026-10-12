package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 本人的会话和私信 HTTP 入口，参与者和最新好友状态由事务服务再次校验。 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RequireLogin
public class ChatController {
    private final ChatService chat;

    @GetMapping("/conversations/{id}")
    public Result<MessageDtos.Conversation> detail(@PathVariable Long id) {
        return Result.success(chat.detail(id));
    }

    @GetMapping("/users/me/friends")
    public Result<PageResult<MessageDtos.UserBrief>> friends(
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size, @RequestParam(required = false) String q) {
        return Result.success(chat.friends(page, size, q));
    }

    @PostMapping("/conversations")
    public Result<MessageDtos.Conversation> open(@Valid @RequestBody MessageDtos.Open request) {
        return Result.success(chat.open(request.friendId()));
    }

    @GetMapping("/conversations")
    public Result<PageResult<MessageDtos.Conversation>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) {
        return Result.success(chat.conversations(page, size));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<MessageDtos.History> history(@PathVariable Long id, @RequestParam(required = false) Long beforeSeq,
                                               @RequestParam(required = false) Long afterSeq, @RequestParam(defaultValue = "50") int size) {
        return Result.success(chat.history(id, beforeSeq, afterSeq, size));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<MessageDtos.Message> send(@PathVariable Long id, @Valid @RequestBody MessageDtos.Send request) {
        return Result.success(chat.send(id, request));
    }

    @PutMapping("/messages/{id}/recall")
    public Result<Void> recall(@PathVariable Long id) {
        chat.recall(id);
        return Result.success();
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> clear(@PathVariable Long id) {
        chat.clear(id);
        return Result.success();
    }

    @PutMapping("/conversations/{id}/read")
    public Result<Void> read(@PathVariable Long id, @Valid @RequestBody MessageDtos.Read request) {
        chat.read(id, request.lastVisibleSeq());
        return Result.success();
    }
}
