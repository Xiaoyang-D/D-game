package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.dto.CheckInResultResp;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.dto.CheckInStatusResp;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import com.xiaoyang.d_game.security.CurrentUser;
import com.xiaoyang.d_game.service.CheckInService;
import com.xiaoyang.d_game.service.GrowthService;
import com.xiaoyang.d_game.service.CheckInRewardRecovery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * 每日签到业务实现。
 *
 * <p>签到状态由 Redis Bitmap 保存，成长奖励由 {@link GrowthService} 发放。
 * 这里负责把“当前登录用户 + 今天”转换为具体签到流程。</p>
 */
public class CheckInServiceImpl implements CheckInService {

    private final CheckInBitmapRepository checkInBitmapRepository;
    private final CheckInRewardRecovery rewardRecovery;

    @Override
    /**
     * 查询签到状态。
     */
    public CheckInStatusResp getStatus() {
        Long userId = currentUserId();
        LocalDate today = CheckInBitmapRepository.today();
        log.debug("查询签到状态, userId={}, date={}", userId, today);
        boolean checkedInToday = checkInBitmapRepository.isCheckedIn(userId, today);

        CheckInStatusResp resp = new CheckInStatusResp();
        resp.setCheckedInToday(checkedInToday);
        resp.setRewardPending(rewardRecovery.isPending(userId, today));
        // 如果今天还没签到，连续天数应从昨天开始算；否则从今天开始算。
        resp.setStreakDays(checkInBitmapRepository.calcStreak(userId,
                checkedInToday ? today : today.minusDays(1)));
        resp.setLastCheckInDate(checkInBitmapRepository.findLastCheckInDate(userId, today));
        log.debug("查询签到状态完成, userId={}, checkedInToday={}, streakDays={}, lastCheckInDate={}",
                userId, resp.isCheckedInToday(), resp.getStreakDays(), resp.getLastCheckInDate());
        return resp;
    }

    @Override
    /**
     * 执行今日签到。
     *
     * <p>先持久化任务，再幂等签到和发奖励；失败任务由用户重试或后台补偿。</p>
     */
    public CheckInResultResp checkIn() {
        Long userId = currentUserId();
        LocalDate today = CheckInBitmapRepository.today();
        log.debug("开始每日签到, userId={}, date={}", userId, today);
        rewardRecovery.enqueue(userId, today);
        CheckInRewardResp reward = rewardRecovery.complete(userId, today);

        // 签到成功后再计算连续天数，此时今天已经被写入 Bitmap。
        int streakDays = checkInBitmapRepository.calcStreak(userId, today);

        CheckInResultResp resp = new CheckInResultResp();
        resp.setCheckInDate(today);
        resp.setStreakDays(streakDays);
        resp.setPointsEarned(reward.getPointsEarned());
        resp.setNewBadges(reward.getNewBadges());
        log.debug("每日签到成功, userId={}, date={}, streakDays={}, pointsEarned={}, newBadges={}",
                userId, today, streakDays, reward.getPointsEarned(), reward.getNewBadges());
        return resp;
    }

    /**
     * 获取当前登录用户 ID。
     */
    private Long currentUserId() {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
