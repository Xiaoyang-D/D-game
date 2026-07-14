package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
/**
 * 签到成长奖励响应。
 *
 * <p>这是服务内部 DTO，由成长服务返回给签到服务。</p>
 */
public class CheckInRewardResp {

    /** 本次奖励积分。 */
    private int pointsEarned;

    /** 本次新增徽章编码列表。 */
    private List<String> newBadges = new ArrayList<>();
}
