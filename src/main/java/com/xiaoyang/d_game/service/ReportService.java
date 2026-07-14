package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.dto.ReportAuditReq;
import com.xiaoyang.d_game.dto.ReportCreateReq;
import com.xiaoyang.d_game.dto.ReportResp;

/**
 * 内容举报服务接口。
 *
 * <p>普通用户创建举报，管理员分页查看待处理举报并填写处理结果。</p>
 */
public interface ReportService {

    /**
     * 创建举报记录。
     */
    void create(ReportCreateReq req);

    /**
     * 分页查询待处理举报。
     */
    PageResult<ReportResp> pagePending(Long page, Long size);

    /**
     * 管理员处理举报。
     */
    void audit(Long reportId, ReportAuditReq req);
}
