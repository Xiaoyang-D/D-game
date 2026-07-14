package com.xiaoyang.d_game.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
/**
 * 审计日志分页查询请求。
 *
 * <p>后台审计页可按操作人、动作、目标和时间范围组合筛选。</p>
 */
public class AuditLogQueryReq {

    /** 操作人用户 ID。 */
    private Long operatorId;

    /** 操作动作编码，例如 BAN_USER、AUDIT_POST。 */
    private String action;

    /** 目标类型，例如 USER、POST。 */
    private String targetType;

    /** 目标 ID。 */
    private Long targetId;

    /** 查询开始时间，ISO 日期时间格式。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    /** 查询结束时间，ISO 日期时间格式。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    /** 页码，从 1 开始。 */
    private Long page = 1L;

    /** 每页条数。 */
    private Long size = 10L;
}
