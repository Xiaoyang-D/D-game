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

@Tag(name = "评论")
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final InteractService interactService;

    @Operation(summary = "帖子评论列表")
    @GetMapping
    public Result<PageResult<CommentResp>> list(@RequestParam Long postId,
                                                @RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "10") Long size) {
        return Result.success(interactService.pageComments(postId, page, size));
    }

    @RequireLogin
    @Operation(summary = "发表评论")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommentCreateReq req) {
        return Result.success(interactService.createComment(req));
    }
}
