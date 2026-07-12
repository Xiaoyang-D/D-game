package com.xiaoyang.d_game.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;
    private Integer targetType;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    private String reason;
    private Integer status;
    private String handleNote;
    private LocalDateTime gmtCreate;
}
