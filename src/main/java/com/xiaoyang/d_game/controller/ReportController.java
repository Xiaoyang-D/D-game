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

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @RequireLogin
    @Operation(summary = "举报内容")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ReportCreateReq req) {
        reportService.create(req);
        return Result.success();
    }
}
