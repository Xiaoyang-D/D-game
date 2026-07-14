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

/**
 * 每日签到接口。
 *
 * <p>签到需要登录，服务层会通过 Redis Bitmap 判断当天是否已签到、计算连续签到天数，并发放积分/徽章奖励。</p>
 */
@Tag(name = "签到")
@RestController
@RequestMapping("/api/v1/check-ins")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 查询当前用户今日签到状态和连续签到信息。
     */
    @RequireLogin
    @Operation(summary = "查询签到状态")
    @GetMapping("/status")
    public Result<CheckInStatusResp> status() {
        return Result.success(checkInService.getStatus());
    }

    /**
     * 执行每日签到。
     *
     * <p>同一自然日只能成功一次；重复签到会返回业务错误码。</p>
     */
    @RequireLogin
    @Operation(summary = "每日签到")
    @PostMapping
    public Result<CheckInResultResp> checkIn() {
        return Result.success(checkInService.checkIn());
    }
}
