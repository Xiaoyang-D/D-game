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

/**
 * 用户互动接口。
 *
 * <p>统一承载点赞、收藏和关注操作。所有写操作都需要登录，服务层会维护去重关系、计数字段和站内通知。</p>
 */
@Tag(name = "互动")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InteractController {

    private final InteractService interactService;

    /**
     * 点赞帖子或评论。
     *
     * <p>目标类型由请求体指定，重复点赞会返回业务错误。</p>
     */
    @RequireLogin
    @Operation(summary = "点赞")
    @PostMapping("/likes")
    public Result<Void> like(@Valid @RequestBody InteractReq req) {
        interactService.like(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    /**
     * 取消点赞。
     *
     * <p>取消时会同步扣减对应目标的点赞数，但计数不会减到负数。</p>
     */
    @RequireLogin
    @Operation(summary = "取消点赞")
    @DeleteMapping("/likes")
    public Result<Void> unlike(@Valid @RequestBody InteractReq req) {
        interactService.unlike(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    /**
     * 收藏目标。
     *
     * <p>当前主要支持收藏帖子和游戏；收藏帖子时会增加帖子收藏数。</p>
     */
    @RequireLogin
    @Operation(summary = "收藏")
    @PostMapping("/favorites")
    public Result<Void> favorite(@Valid @RequestBody InteractReq req) {
        interactService.favorite(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    /**
     * 取消收藏目标。
     */
    @RequireLogin
    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites")
    public Result<Void> unfavorite(@Valid @RequestBody InteractReq req) {
        interactService.unfavorite(req.getTargetType(), req.getTargetId());
        return Result.success();
    }

    /**
     * 关注指定用户。
     *
     * <p>不能关注自己；关注成功后会给被关注者发送站内通知。</p>
     */
    @RequireLogin
    @Operation(summary = "关注用户")
    @PostMapping("/follows/{followeeId}")
    public Result<Void> follow(@PathVariable Long followeeId) {
        interactService.follow(followeeId);
        return Result.success();
    }

    /**
     * 取消关注指定用户。
     */
    @RequireLogin
    @Operation(summary = "取消关注")
    @DeleteMapping("/follows/{followeeId}")
    public Result<Void> unfollow(@PathVariable Long followeeId) {
        interactService.unfollow(followeeId);
        return Result.success();
    }
}
