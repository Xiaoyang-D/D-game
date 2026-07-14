package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.ReportCreateReq;
import com.xiaoyang.d_game.security.RequireLogin;
import com.xiaoyang.d_game.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户举报接口。
 *
 * <p>登录用户可以举报帖子、评论等内容。举报记录会进入后台待处理列表，
 * 同一用户对同一目标重复举报会被唯一约束拦截。</p>
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    /**
     * 创建举报记录。
     */
    @RequireLogin
    @Operation(summary = "举报内容")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ReportCreateReq req) {
        reportService.create(req);
        return Result.success();
    }
}
