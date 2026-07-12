package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InteractReq {

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}
