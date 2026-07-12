package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.InteractReq;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.InteractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "互动")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InteractController {

    private final InteractService interactService;

    @RequireLogin
    @Operation(summary = "点赞")
    @PostMapping("/likes")
    public Result<Void> like(@Valid @RequestBody InteractReq req) {
        interactService.like(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "取消点赞")
    @DeleteMapping("/likes")
    public Result<Void> unlike(@Valid @RequestBody InteractReq req) {
        interactService.unlike(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "收藏")
    @PostMapping("/favorites")
    public Result<Void> favorite(@Valid @RequestBody InteractReq req) {
        interactService.favorite(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites")
    public Result<Void> unfavorite(@Valid @RequestBody InteractReq req) {
        interactService.unfavorite(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "关注用户")
    @PostMapping("/follows/{followeeId}")
    public Result<Void> follow(@PathVariable Long followeeId) {
        interactService.follow(followeeId);
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "取消关注")
    @DeleteMapping("/follows/{followeeId}")
    public Result<Void> unfollow(@PathVariable Long followeeId) {
        interactService.unfollow(followeeId);
        return Result.success();
    }
}
