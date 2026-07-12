package com.xiaoyang.d_game.dto;

import lombok.Data;

@Data
public class TokenResp {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private UserResp user;
}
