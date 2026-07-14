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

/**
 * 帖子接口。
 *
 * <p>前台可浏览已审核通过的帖子、查看详情和排行榜；登录用户可查看自己的帖子、关注流和发帖。
 * 发帖后默认进入待审核状态，由后台审核通过后才会出现在公开列表中。</p>
 */
@Tag(name = "帖子")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 分页查询公开帖子列表。
     *
     * <p>只返回审核通过的帖子，可按版块、关联游戏和标题关键字过滤。</p>
     */
    @Operation(summary = "分页查询帖子")
    @GetMapping
    public Result<PageResult<PostResp>> page(PostQueryReq req) {
        return Result.success(postService.pagePosts(req));
    }

    /**
     * 查询当前登录用户自己的帖子。
     *
     * <p>会返回用户自己的所有状态帖子，方便前端展示待审核、已通过或被拒绝状态。</p>
     */
    @RequireLogin
    @Operation(summary = "我的帖子")
    @GetMapping("/mine")
    public Result<PageResult<PostResp>> mine(@RequestParam(defaultValue = "1") Long page,
                                             @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageMyPosts(page, size));
    }

    /**
     * 查询关注用户发布的帖子。
     *
     * <p>只展示关注对象已审核通过的帖子，构成用户的关注动态流。</p>
     */
    @RequireLogin
    @Operation(summary = "关注用户的帖子")
    @GetMapping("/following")
    public Result<PageResult<PostResp>> following(@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageFollowingPosts(page, size));
    }

    /**
     * 查询帖子排行榜。
     *
     * <p>支持最新和热度两种排序；热度由浏览、点赞、评论和收藏加权计算。</p>
     */
    @Operation(summary = "帖子排行榜")
    @GetMapping("/ranking")
    public Result<PageResult<PostResp>> ranking(PostRankingQueryReq req) {
        return Result.success(postService.pageRanking(req));
    }

    /**
     * 查询帖子详情。
     *
     * <p>详情接口会增加浏览数。未审核通过的帖子只允许作者本人或管理员查看。</p>
     */
    @Operation(summary = "帖子详情")
    @GetMapping("/{id}")
    public Result<PostResp> detail(@PathVariable String id) {
        return Result.success(postService.getPostDetail(Long.parseLong(id)));
    }

    /**
     * 创建帖子。
     *
     * <p>服务层会校验版块和关联游戏是否存在，清洗富文本内容，并把新帖置为待审核。</p>
     */
    @RequireLogin
    @Operation(summary = "发帖")
    @PostMapping
    public Result<String> create(@Valid @RequestBody PostCreateReq req) {
        return Result.success(postService.createPost(req));
    }
}
