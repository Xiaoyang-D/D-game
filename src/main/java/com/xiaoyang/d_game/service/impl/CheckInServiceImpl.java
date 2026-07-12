package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.dto.CheckInResultResp;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.dto.CheckInStatusResp;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import com.xiaoyang.d_game.security.UserContext;
import com.xiaoyang.d_game.service.CheckInService;
import com.xiaoyang.d_game.service.GrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final CheckInBitmapRepository checkInBitmapRepository;
    private final GrowthService growthService;

    @Override
    public CheckInStatusResp getStatus() {
        Long userId = currentUserId();
        LocalDate today = CheckInBitmapRepository.today();
        log.debug("查询签到状态, userId={}, date={}", userId, today);
        boolean checkedInToday = checkInBitmapRepository.isCheckedIn(userId, today);

        CheckInStatusResp resp = new CheckInStatusResp();
        resp.setCheckedInToday(checkedInToday);
        resp.setStreakDays(checkInBitmapRepository.calcStreak(userId,
                checkedInToday ? today : today.minusDays(1)));
        resp.setLastCheckInDate(checkInBitmapRepository.findLastCheckInDate(userId, today));
        log.debug("查询签到状态完成, userId={}, checkedInToday={}, streakDays={}, lastCheckInDate={}",
                userId, resp.isCheckedInToday(), resp.getStreakDays(), resp.getLastCheckInDate());
        return resp;
    }

    @Override
    public CheckInResultResp checkIn() {
        Long userId = currentUserId();
        LocalDate today = CheckInBitmapRepository.today();
        log.debug("开始每日签到, userId={}, date={}", userId, today);
        checkInBitmapRepository.markCheckedIn(userId, today);

        int streakDays = checkInBitmapRepository.calcStreak(userId, today);
        CheckInRewardResp reward = growthService.onCheckInSuccess(userId, today, streakDays);

        CheckInResultResp resp = new CheckInResultResp();
        resp.setCheckInDate(today);
        resp.setStreakDays(streakDays);
        resp.setPointsEarned(reward.getPointsEarned());
        resp.setNewBadges(reward.getNewBadges());
        log.debug("每日签到成功, userId={}, date={}, streakDays={}, pointsEarned={}, newBadges={}",
                userId, today, streakDays, reward.getPointsEarned(), reward.getNewBadges());
        return resp;
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
