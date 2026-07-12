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

@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GrowthService growthService;

    @RequireLogin
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserResp> me() {
        return Result.success(userService.getCurrentUserProfile());
    }

    @RequireLogin
    @Operation(summary = "获取当前用户成长信息")
    @GetMapping("/me/growth")
    public Result<UserGrowthResp> growth() {
        return Result.success(growthService.getUserGrowth(UserContext.getUserId()));
    }

    @RequireLogin
    @Operation(summary = "更新个人资料")
    @PutMapping("/me")
    public Result<UserResp> updateProfile(@Valid @RequestBody UpdateProfileReq req) {
        return Result.success(userService.updateProfile(req));
    }
}
