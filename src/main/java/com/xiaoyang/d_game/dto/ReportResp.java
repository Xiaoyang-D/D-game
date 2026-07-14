package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 举报响应。
 */
public class ReportResp {

    /** 举报记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 举报人用户 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;

    /** 举报目标类型。 */
    private Integer targetType;

    /** 举报目标 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /** 举报原因。 */
    private String reason;

    /** 处理状态：0 待处理，1 已处理，2 已驳回。 */
    private Integer status;

    /** 管理员处理备注。 */
    private String handleNote;

    /** 举报创建时间。 */
    private LocalDateTime gmtCreate;
}
