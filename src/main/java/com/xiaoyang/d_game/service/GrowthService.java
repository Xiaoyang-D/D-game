package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.enums.BadgeCodeEnum;
import com.xiaoyang.d_game.common.enums.PointSourceEnum;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.dto.UserGrowthResp;

import java.time.LocalDate;

public interface GrowthService {

    int grantPoints(Long userId, PointSourceEnum source, String sourceKey, int points, String remark);

    boolean grantBadge(Long userId, BadgeCodeEnum badge);

    CheckInRewardResp onCheckInSuccess(Long userId, LocalDate date, int streakDays);

    UserGrowthResp getUserGrowth(Long userId);
}
