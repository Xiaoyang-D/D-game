package com.xiaoyang.d_game.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AuditLogQueryReq {

    private Long operatorId;

    private String action;

    private String targetType;

    private Long targetId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private Long page = 1L;

    private Long size = 10L;
}
