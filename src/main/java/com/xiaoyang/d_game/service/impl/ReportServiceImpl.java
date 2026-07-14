package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.PageResult;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.common.enums.TargetTypeEnum;
import com.xiaoyang.d_game.dto.ReportAuditReq;
import com.xiaoyang.d_game.dto.ReportCreateReq;
import com.xiaoyang.d_game.dto.ReportResp;
import com.xiaoyang.d_game.entity.ContentReport;
import com.xiaoyang.d_game.mapper.ContentReportMapper;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * 内容举报业务实现。
 *
 * <p>普通用户创建举报后进入待处理状态；管理员处理后改为已处理或已驳回。
 * 同一举报人对同一目标只能举报一次，避免刷屏。</p>
 */
public class ReportServiceImpl implements ReportService {

    /** 举报待处理。 */
    private static final int PENDING = 0;

    /** 举报已处理。 */
    private static final int HANDLED = 1;

    /** 举报已驳回。 */
    private static final int DISMISSED = 2;

    private final ContentReportMapper contentReportMapper;

    @Override
    /**
     * 创建举报。
     */
    public void create(ReportCreateReq req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        // 校验目标类型是否合法；返回值不用保存，只要非法时抛异常。
        TargetTypeEnum.of(req.getTargetType());
        boolean duplicate = contentReportMapper.exists(new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getReporterId, userId).eq(ContentReport::getTargetType, req.getTargetType())
                .eq(ContentReport::getTargetId, req.getTargetId()));
        if (duplicate) {
            throw new BizException(ResultCode.CONFLICT, "请勿重复举报同一内容");
        }
        ContentReport report = new ContentReport();
        report.setReporterId(userId);
        report.setTargetType(req.getTargetType());
        report.setTargetId(req.getTargetId());
        report.setReason(req.getReason().trim());
        report.setStatus(PENDING);
        contentReportMapper.insert(report);
    }

    @Override
    /**
     * 分页查询待处理举报。
     */
    public PageResult<ReportResp> pagePending(Long page, Long size) {
        Page<ContentReport> result = contentReportMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getStatus, PENDING).orderByAsc(ContentReport::getGmtCreate));
        PageResult<ReportResp> response = new PageResult<>();
        response.setPage(result.getCurrent());
        response.setSize(result.getSize());
        response.setTotal(result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::toResp).toList());
        return response;
    }

    @Override
    /**
     * 处理举报。
     */
    public void audit(Long reportId, ReportAuditReq req) {
        ContentReport report = contentReportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        report.setStatus(req.getHandled() ? HANDLED : DISMISSED);
        report.setHandleNote(req.getNote());
        // 处理人来自当前管理员登录态，用于后台追踪处理责任。
        report.setHandlerId(UserContext.getUserId());
        contentReportMapper.updateById(report);
    }

    /**
     * 转换举报响应对象。
     */
    private ReportResp toResp(ContentReport report) {
        ReportResp response = new ReportResp();
        response.setId(report.getId());
        response.setReporterId(report.getReporterId());
        response.setTargetType(report.getTargetType());
        response.setTargetId(report.getTargetId());
        response.setReason(report.getReason());
        response.setStatus(report.getStatus());
        response.setHandleNote(report.getHandleNote());
        response.setGmtCreate(report.getGmtCreate());
        return response;
    }
}
