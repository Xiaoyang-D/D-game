package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
/**
 * 用户成长信息响应。
 */
public class UserGrowthResp {

    /** 用户累计积分。 */
    private int totalPoints;

    /** 当前连续签到天数。 */
    private int streakDays;

    /** 今天是否已签到。 */
    private boolean checkedInToday;

    /** 已获得徽章列表。 */
    private List<BadgeResp> badges = new ArrayList<>();
}
