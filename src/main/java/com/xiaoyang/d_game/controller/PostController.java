package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.PostCreateReq;
import com.xiaoyang.d_game.dto.PostDraftResp;
import com.xiaoyang.d_game.dto.PostDraftSaveReq;
import com.xiaoyang.d_game.dto.PostManageItemResp;
import com.xiaoyang.d_game.dto.PostManagePageResp;
import com.xiaoyang.d_game.dto.PostQueryReq;
import com.xiaoyang.d_game.dto.PostRankingQueryReq;
import com.xiaoyang.d_game.dto.PostResp;
import com.xiaoyang.d_game.dto.PostPublishReq;
import com.xiaoyang.d_game.dto.PostTopicResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @RequireLogin
    @Operation(summary = "我的发布管理列表")
    @GetMapping("/manage")
    /**
     * 按审核状态分页查询当前用户的帖子，并同时返回四种状态的数量。
     */
    public Result<PostManagePageResp> manage(@RequestParam(defaultValue = "APPROVED") String status,
                                              @RequestParam(defaultValue = "1") Long page,
                                              @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageMyPostManagement(status, page, size));
    }

    @RequireLogin
    @Operation(summary = "我的发布管理详情")
    @GetMapping("/manage/{id}")
    /** 读取当前用户自己的帖子，用于管理页编辑回填。 */
    public Result<PostManageItemResp> manageDetail(@PathVariable Long id) {
        return Result.success(postService.getMyManagedPost(id));
    }

    @RequireLogin
    @Operation(summary = "更新我的已提交帖子")
    @PutMapping("/manage/{id}")
    /** 更新帖子内容；非草稿状态会重新进入管理员审核队列。 */
    public Result<PostManageItemResp> updateManagedPost(@PathVariable Long id,
                                                         @Valid @RequestBody PostPublishReq req) {
        return Result.success(postService.updateMyManagedPost(id, req));
    }

    @RequireLogin
    @Operation(summary = "删除我的已提交帖子")
    @DeleteMapping("/manage/{id}")
    /** 删除当前用户的帖子，所有状态统一使用逻辑删除。 */
    public Result<Void> deleteManagedPost(@PathVariable Long id) {
        postService.deleteMyManagedPost(id);
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "我的草稿列表")
    @GetMapping("/drafts")
    public Result<PageResult<PostDraftResp>> drafts(@RequestParam(defaultValue = "1") Long page,
                                                    @RequestParam(defaultValue = "10") Long size) {
        return Result.success(postService.pageMyDrafts(page, size));
    }

    @RequireLogin
    @Operation(summary = "草稿详情")
    @GetMapping("/drafts/{id}")
    public Result<PostDraftResp> draft(@PathVariable Long id) {
        return Result.success(postService.getMyDraft(id));
    }

    @RequireLogin
    @Operation(summary = "保存草稿")
    @PostMapping("/drafts")
    public Result<PostDraftResp> saveDraft(@Valid @RequestBody PostDraftSaveReq req) {
        return Result.success(postService.saveDraft(req));
    }

    @RequireLogin
    @Operation(summary = "更新草稿")
    @PutMapping("/drafts/{id}")
    public Result<PostDraftResp> updateDraft(@PathVariable Long id,
                                             @Valid @RequestBody PostDraftSaveReq req) {
        return Result.success(postService.updateDraft(id, req));
    }

    @RequireLogin
    @Operation(summary = "删除草稿")
    @DeleteMapping("/drafts/{id}")
    public Result<Void> deleteDraft(@PathVariable Long id) {
        postService.deleteDraft(id);
        return Result.success();
    }

    @RequireLogin
    @Operation(summary = "提交草稿")
    @PostMapping("/drafts/{id}/publish")
    public Result<String> publishDraft(@PathVariable Long id,
                                       @Valid @RequestBody PostPublishReq req) {
        return Result.success(postService.publishDraft(id, req));
    }

    @RequireLogin
    @Operation(summary = "发布帖子")
    @PostMapping("/publish")
    public Result<String> publish(@Valid @RequestBody PostPublishReq req) {
        return Result.success(postService.publishPost(req));
    }

    @Operation(summary = "搜索社区话题")
    @GetMapping("/topics")
    public Result<List<PostTopicResp>> topics(@RequestParam(required = false) String keyword) {
        return Result.success(postService.searchTopics(keyword));
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
