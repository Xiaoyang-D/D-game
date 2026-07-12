package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.ReportAuditReq;
import com.xiaoyang.d_game.dto.ReportResp;
import com.xiaoyang.d_game.security.RequireRole;
import com.xiaoyang.d_game.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    @GetMapping("/pending")
    public Result<PageResult<ReportResp>> pending(@RequestParam(defaultValue = "1") Long page,
                                                   @RequestParam(defaultValue = "10") Long size) {
        return Result.success(reportService.pagePending(page, size));
    }

    @PutMapping("/{id}")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ReportAuditReq req) {
        reportService.audit(id, req);
        return Result.success();
    }
}
