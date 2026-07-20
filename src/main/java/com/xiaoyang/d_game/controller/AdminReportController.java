package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.Result;
import com.xiaoyang.d_game.dto.ReportAuditReq;
import com.xiaoyang.d_game.dto.ReportResp;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * 后台举报处理接口。
 *
 * <p>所有接口都需要 ADMIN 角色。普通用户提交举报后，管理员通过这里分页查看待处理举报并写入处理结果。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {
    private final ReportService reportService;

    /**
     * 分页查询待处理举报。
     *
     * <p>只返回待处理状态的举报，便于后台处理队列。</p>
     */
    @GetMapping("/pending")
    public Result<PageResult<ReportResp>> pending(@RequestParam(defaultValue = "1") Long page,
                                                   @RequestParam(defaultValue = "10") Long size) {
        return Result.success(reportService.pagePending(page, size));
    }

    /**
     * 审核并处理举报。
     *
     * <p>请求体中包含处理状态和处理备注，服务层会记录处理人 ID 与处理说明。</p>
     */
    @PutMapping("/{id}")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody ReportAuditReq req) {
        reportService.audit(id, req);
        return Result.success();
    }
}
