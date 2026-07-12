package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.CheckInResultResp;
import com.xiaoyang.d_game.dto.CheckInStatusResp;

public interface CheckInService {

    CheckInStatusResp getStatus();

    CheckInResultResp checkIn();
}
