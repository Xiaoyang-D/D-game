package com.xiaoyang.d_game.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
/**
 * 当前用户签到状态响应。
 */
public class CheckInStatusResp {

    /** 今天是否已经签到。 */
    private boolean checkedInToday;

    /** 签到意图已保存，但奖励流程尚未确认完成，可以重试。 */
    private boolean rewardPending;

    /** 当前连续签到天数。 */
    private int streakDays;

    /** 最近一次签到日期；从未签到时为空。 */
    private LocalDate lastCheckInDate;
}
