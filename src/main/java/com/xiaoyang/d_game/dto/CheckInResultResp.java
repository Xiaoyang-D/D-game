package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
/**
 * 每日签到结果响应。
 *
 * <p>用于前端展示本次签到日期、连续签到天数和获得奖励。</p>
 */
public class CheckInResultResp {

    /** 本次签到日期。 */
    private LocalDate checkInDate;

    /** 签到成功后的连续签到天数。 */
    private int streakDays;

    /** 本次签到获得的积分。 */
    private int pointsEarned;

    /** 本次新获得的徽章编码列表。 */
    private List<String> newBadges = new ArrayList<>();
}
