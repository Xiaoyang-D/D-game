package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.CommentCreateReq;
import com.xiaoyang.d_game.dto.CommentResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.InteractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口。
 *
 * <p>评论属于互动模块的一部分：读取评论对游客开放，发表评论需要登录。
 * 当前实现支持顶级评论和楼中楼，父评论 ID 为 0 或空表示顶级评论。</p>
 */
@Tag(name = "评论")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final InteractService interactService;

    /**
     * 分页查询帖子评论。
     *
     * <p>只返回审核通过的评论，并补充评论作者昵称。</p>
     */
    @Operation(summary = "帖子评论列表")
    @GetMapping
    public Result<PageResult<CommentResp>> list(@RequestParam Long postId,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "10") Long size) {
        return Result.success(interactService.pageComments(postId, page, size));
    }

    /**
     * 发表评论。
     *
     * <p>发表成功后会增加帖子评论数，并给帖子作者发送站内通知。</p>
     */
    @RequireLogin
    @Operation(summary = "发表评论")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommentCreateReq req) {
        return Result.success(interactService.createComment(req));
    }
}
