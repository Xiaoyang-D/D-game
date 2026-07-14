package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 审计日志响应。
 */
public class AuditLogResp {

    /** 审计日志 ID。 */
    private Long id;

    /** 操作人用户 ID。 */
    private Long operatorId;

    /** 操作人昵称，用户不存在时为空。 */
    private String operatorNickname;

    /** 操作动作编码。 */
    private String action;

    /** 被操作目标类型。 */
    private String targetType;

    /** 被操作目标 ID。 */
    private Long targetId;

    /** 操作详情。 */
    private String detail;

    /** 操作发生时间。 */
    private LocalDateTime gmtCreate;
}
