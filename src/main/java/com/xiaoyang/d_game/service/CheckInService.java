package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.dto.CheckInResultResp;
import com.xiaoyang.d_game.dto.CheckInStatusResp;

/**
 * 每日签到业务接口。
 *
 * <p>签到状态存储在 Redis Bitmap 中，签到成功后会触发成长体系发放积分和徽章。</p>
 */
public interface CheckInService {

    /**
     * 查询当前登录用户的签到状态。
     */
    CheckInStatusResp getStatus();

    /**
     * 完成当前登录用户今日签到，并返回奖励结果。
     */
    CheckInResultResp checkIn();
}
