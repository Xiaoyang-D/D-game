package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportAuditReq {
    @NotNull
    private Boolean handled;
    @Size(max = 500)
    private String note;
}
