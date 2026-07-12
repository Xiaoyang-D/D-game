package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "帖子")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "分页查询帖子")
    @GetMapping
    public Result<PageResult<PostResp>> page(PostQueryReq req) {
        return Result.success(postService.pagePosts(req));
    }

    @RequireLogin
    @Operation(summary = "我的帖子")
    @GetMapping("/mine")
    public Result<PageResult<PostResp>> mine(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageMyPosts(page, size));
    }

    @RequireLogin
    @Operation(summary = "关注用户的帖子")
    @GetMapping("/following")
    public Result<PageResult<PostResp>> following(@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageFollowingPosts(page, size));
    }

    @Operation(summary = "帖子排行榜")
    @GetMapping("/ranking")
    public Result<PageResult<PostResp>> ranking(PostRankingQueryReq req) {
        return Result.success(postService.pageRanking(req));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/{id}")
    public Result<PostResp> detail(@PathVariable String id) {
        return Result.success(postService.getPostDetail(Long.parseLong(id)));
    }

    @RequireLogin
    @Operation(summary = "发帖")
    @PostMapping
    public Result<String> create(@Valid @RequestBody PostCreateReq req) {
        return Result.success(postService.createPost(req));
    }
}
