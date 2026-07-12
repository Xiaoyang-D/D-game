package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResp {

    private Long id;

    private Long operatorId;

    private String operatorNickname;

    private String action;

    private String targetType;

    private Long targetId;

    private String detail;

    private LocalDateTime gmtCreate;
}
