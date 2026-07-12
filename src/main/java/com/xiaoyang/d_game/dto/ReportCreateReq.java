package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCreateReq {
    @NotNull
    private Integer targetType;
    @NotNull
    private Long targetId;
    @NotBlank
    @Size(max = 500)
    private String reason;
}
