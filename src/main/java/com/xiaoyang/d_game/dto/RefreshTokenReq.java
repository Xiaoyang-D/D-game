package com.xiaoyang.d_game.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenReq {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
