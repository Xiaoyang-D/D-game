package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.LoginReq;
import com.xiaoyang.d_game.dto.RefreshTokenReq;
import com.xiaoyang.d_game.dto.RegisterReq;
import com.xiaoyang.d_game.dto.TokenResp;

public interface AuthService {

    TokenResp register(RegisterReq req);

    TokenResp login(LoginReq req);

    TokenResp refresh(RefreshTokenReq req);
}
