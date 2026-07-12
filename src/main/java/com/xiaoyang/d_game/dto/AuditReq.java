package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditReq {

    @NotNull(message = "是否通过不能为空")
    private Boolean approved;

    private String reason;
}
