package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.CheckInResultResp;
import com.xiaoyang.d_game.dto.CheckInStatusResp;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "签到")
@RestController
@RequestMapping("/api/v1/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @RequireLogin
    @Operation(summary = "查询签到状态")
    @GetMapping("/status")
    public Result<CheckInStatusResp> status() {
        return Result.success(checkInService.getStatus());
    }

    @RequireLogin
    @Operation(summary = "每日签到")
    @PostMapping
    public Result<CheckInResultResp> checkIn() {
        return Result.success(checkInService.checkIn());
    }
}
