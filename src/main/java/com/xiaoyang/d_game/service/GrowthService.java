package com.xiaoyang.d_game.service;

import com.xiaoyang.d_game.common.enums.BadgeCodeEnum;
import com.xiaoyang.d_game.common.enums.PointSourceEnum;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.dto.UserGrowthResp;

import java.time.LocalDate;

/**
 * 用户成长体系服务。
 *
 * <p>负责积分流水、徽章发放和等级计算。积分发放使用业务幂等键防重复，
 * 徽章表使用用户 ID + 徽章编码唯一约束防重复。</p>
 */
public interface GrowthService {

    /**
     * 发放积分。
     *
     * @param userId 获得积分的用户
     * @param source 积分来源类型
     * @param sourceKey 幂等业务键，同一用户同一 key 只会成功发放一次
     * @param points 积分变化值，可正可负
     * @param remark 后台可读的说明
     * @return 实际发放积分；重复发放时通常返回 0
     */
    int grantPoints(Long userId, PointSourceEnum source, String sourceKey, int points, String remark);

    /**
     * 发放徽章。
     *
     * @return true 表示本次新获得徽章，false 表示之前已经拥有
     */
    boolean grantBadge(Long userId, BadgeCodeEnum badge);

    /**
     * 签到成功后的成长奖励入口。
     *
     * <p>会根据签到日期和连续天数发放基础积分、连续签到奖励和徽章。</p>
     */
    CheckInRewardResp onCheckInSuccess(Long userId, LocalDate date, int streakDays);

    /**
     * 汇总用户成长信息。
     */
    UserGrowthResp getUserGrowth(Long userId);
}
