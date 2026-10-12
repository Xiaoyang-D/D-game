package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.MessageDtos;
import com.xiaoyang.d_game.service.MentionService;
import com.xiaoyang.d_game.security.RequireLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 登录后查询提及候选和评论位置，搜索响应仅包含用户展示信息。 */
@RestController
@RequiredArgsConstructor
@RequireLogin
public class MentionController {
    private final MentionService mentions;

    @GetMapping("/api/v1/users/mention-candidates")
    public Result<List<MessageDtos.UserBrief>> candidates(@RequestParam String q) {
        return Result.success(mentions.candidates(q));
    }

    @GetMapping("/api/v1/comments/{id}/location")
    public Result<MessageDtos.Location> location(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "DEFAULT") String sort, @RequestParam(defaultValue = "20") int size) {
        return Result.success(mentions.location(id, sort, size));
    }
}
