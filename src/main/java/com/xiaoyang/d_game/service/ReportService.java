package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.ReportAuditReq;
import com.xiaoyang.d_game.dto.ReportCreateReq;
import com.xiaoyang.d_game.dto.ReportResp;

public interface ReportService {
    void create(ReportCreateReq req);
    PageResult<ReportResp> pagePending(Long page, Long size);
    void audit(Long reportId, ReportAuditReq req);
}
