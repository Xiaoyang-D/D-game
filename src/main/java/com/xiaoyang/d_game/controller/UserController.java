package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.PostCollectionCreateReq;
import com.xiaoyang.d_game.dto.PostCollectionResp;
import com.xiaoyang.d_game.dto.UserFavoriteResp;
import com.xiaoyang.d_game.dto.UserGrowthResp;
import com.xiaoyang.d_game.dto.UserProfileResp;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.GrowthService;
import com.xiaoyang.d_game.service.PostService;
import com.xiaoyang.d_game.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户接口。
 *
 * <p>这里同时保留“我的资料编辑页”和“公开用户主页”两类接口：
 * /me 系列只给登录用户自己用；/users/{id} 系列给公开主页使用。</p>
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GrowthService growthService;
    private final PostService postService;

    /**
     * 获取当前登录用户的个人资料。
     */
    @RequireLogin
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserResp> me() {
        return Result.success(userService.getCurrentUserProfile());
    }

    /**
     * 获取当前登录用户的成长信息。
     */
    @RequireLogin
    @Operation(summary = "获取当前用户成长信息")
    @GetMapping("/me/growth")
    public Result<UserGrowthResp> growth() {
        return Result.success(growthService.getUserGrowth(CurrentUser.getUserId()));
    }

    @RequireLogin
    @Operation(summary = "我的个人合集")
    @GetMapping("/me/collections")
    public Result<List<PostCollectionResp>> collections() {
        return Result.success(postService.listMyCollections());
    }

    @RequireLogin
    @Operation(summary = "创建个人合集")
    @PostMapping("/me/collections")
    public Result<PostCollectionResp> createCollection(@Valid @RequestBody PostCollectionCreateReq req) {
        return Result.success(postService.createMyCollection(req.getName()));
    }

    /**
     * 获取公开用户主页数据。
     *
     * <p>这个接口不强制登录，但如果当前访问者已经登录，后端会顺手返回 isFollowing，
     * 方便前端直接渲染关注按钮状态。</p>
     */
    @Operation(summary = "获取公开用户资料")
    @GetMapping("/{id}/profile")
    public Result<UserProfileResp> publicProfile(@PathVariable Long id) {
        return Result.success(userService.getPublicProfile(id));
    }

    /**
     * 分页查询公开收藏流。
     *
     * <p>收藏流只展示公开可见内容，帖子和游戏混排返回，前端再按 targetType 区分样式。</p>
     */
    @Operation(summary = "分页查询公开收藏")
    @GetMapping("/{id}/favorites")
    public Result<PageResult<UserFavoriteResp>> favorites(@PathVariable Long id,
                                                           @RequestParam(defaultValue = "1") Long page,
                                                           @RequestParam(defaultValue = "10") Long size) {
        return Result.success(userService.pagePublicFavorites(id, page, size));
    }

    /**
     * 更新当前登录用户个人资料。
     */
    @RequireLogin
    @Operation(summary = "更新个人资料")
    @PutMapping("/me")
    public Result<UserResp> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        return Result.success(userService.updateProfile(req));
    }
}
