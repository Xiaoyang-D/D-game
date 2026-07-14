package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.UpdateProfileReq;
import com.xiaoyang.d_game.dto.UserGrowthResp;
import com.xiaoyang.d_game.dto.UserResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.GrowthService;
import com.xiaoyang.d_game.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户接口。
 *
 * <p>所有接口都围绕当前登录用户，不从前端接收用户 ID，避免越权读取或修改他人资料。
 * 用户成长信息由积分流水和徽章表聚合得到。</p>
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GrowthService growthService;

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
     * 获取当前用户成长信息。
     *
     * <p>包含总积分、等级和已获得徽章，用于个人中心展示。</p>
     */
    @RequireLogin
    @Operation(summary = "获取当前用户成长信息")
    @GetMapping("/me/growth")
    public Result<UserGrowthResp> growth() {
        return Result.success(growthService.getUserGrowth(UserContext.getUserId()));
    }

    /**
     * 更新当前用户个人资料。
     *
     * <p>只允许更新昵称、头像、简介等资料字段，不允许通过该接口修改用户名、密码哈希或角色。</p>
     */
    @RequireLogin
    @Operation(summary = "更新个人资料")
    @PutMapping("/me")
    public Result<UserResp> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        return Result.success(userService.updateProfile(req));
    }
}
