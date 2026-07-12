package com.xiaoyang.d_game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyang.d_game.common.enums.BadgeCodeEnum;
import com.xiaoyang.d_game.common.enums.PointSourceEnum;
import com.xiaoyang.d_game.dto.BadgeResp;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.dto.UserGrowthResp;
import com.xiaoyang.d_game.entity.UserBadge;
import com.xiaoyang.d_game.entity.UserPointLog;
import com.xiaoyang.d_game.mapper.UserBadgeMapper;
import com.xiaoyang.d_game.mapper.UserPointLogMapper;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import com.xiaoyang.d_game.service.GrowthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GrowthServiceImpl implements GrowthService {

    private final UserPointLogMapper userPointLogMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final CheckInBitmapRepository checkInBitmapRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantPoints(Long userId, PointSourceEnum source, String sourceKey, int points, String remark) {
        String fullKey = source.getCode() + ":" + sourceKey;
        log.debug("准备发放积分, userId={}, source={}, sourceKey={}, points={}",
                userId, source.getCode(), fullKey, points);
        UserPointLog existing = userPointLogMapper.selectOne(new LambdaQueryWrapper<UserPointLog>()
                .eq(UserPointLog::getUserId, userId)
                .eq(UserPointLog::getSourceKey, fullKey)
                .last("LIMIT 1"));
        if (existing != null) {
            log.debug("积分已发放过, userId={}, sourceKey={}", userId, fullKey);
            return 0;
        }
        UserPointLog pointLog = new UserPointLog();
        pointLog.setUserId(userId);
        pointLog.setSourceType(source.getCode());
        pointLog.setSourceKey(fullKey);
        pointLog.setPointsChange(points);
        pointLog.setRemark(remark);
        userPointLogMapper.insert(pointLog);
        log.debug("积分发放成功, userId={}, sourceKey={}, points={}", userId, fullKey, points);
        return points;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean grantBadge(Long userId, BadgeCodeEnum badge) {
        log.debug("准备发放徽章, userId={}, badgeCode={}", userId, badge.getCode());
        UserBadge existing = userBadgeMapper.selectOne(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeCode, badge.getCode())
                .last("LIMIT 1"));
        if (existing != null) {
            log.debug("徽章已拥有, userId={}, badgeCode={}", userId, badge.getCode());
            return false;
        }
        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(userId);
        userBadge.setBadgeCode(badge.getCode());
        userBadge.setBadgeName(badge.getName());
        userBadge.setDescription(badge.getDescription());
        userBadgeMapper.insert(userBadge);
        log.debug("徽章发放成功, userId={}, badgeCode={}", userId, badge.getCode());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInRewardResp onCheckInSuccess(Long userId, LocalDate date, int streakDays) {
        CheckInRewardResp reward = new CheckInRewardResp();
        int points = grantPoints(userId, PointSourceEnum.CHECK_IN, date.toString(),
                PointSourceEnum.CHECK_IN.getDefaultPoints(), "每日签到");
        reward.setPointsEarned(points);

        List<String> newBadges = new ArrayList<>();
        if (grantBadge(userId, BadgeCodeEnum.FIRST_CHECK_IN)) {
            newBadges.add(BadgeCodeEnum.FIRST_CHECK_IN.getCode());
        }
        if (streakDays >= 7 && grantBadge(userId, BadgeCodeEnum.STREAK_7)) {
            newBadges.add(BadgeCodeEnum.STREAK_7.getCode());
        }
        reward.setNewBadges(newBadges);
        return reward;
    }

    @Override
    public UserGrowthResp getUserGrowth(Long userId) {
        log.debug("查询用户成长信息, userId={}", userId);
        LocalDate today = CheckInBitmapRepository.today();
        boolean checkedInToday = checkInBitmapRepository.isCheckedIn(userId, today);
        int streakDays = checkInBitmapRepository.calcStreak(userId,
                checkedInToday ? today : today.minusDays(1));

        List<UserPointLog> pointLogs = userPointLogMapper.selectList(new LambdaQueryWrapper<UserPointLog>()
                .eq(UserPointLog::getUserId, userId));
        int totalPoints = pointLogs.stream()
                .mapToInt(UserPointLog::getPointsChange)
                .sum();

        List<UserBadge> badges = userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .orderByAsc(UserBadge::getGmtCreate));

        UserGrowthResp resp = new UserGrowthResp();
        resp.setTotalPoints(totalPoints);
        resp.setStreakDays(streakDays);
        resp.setCheckedInToday(checkedInToday);
        resp.setBadges(badges.stream().map(this::toBadgeResp).toList());
        log.debug("查询用户成长信息完成, userId={}, totalPoints={}, streakDays={}, badgeCount={}",
                userId, totalPoints, streakDays, badges.size());
        return resp;
    }

    private BadgeResp toBadgeResp(UserBadge badge) {
        BadgeResp resp = new BadgeResp();
        resp.setBadgeCode(badge.getBadgeCode());
        resp.setBadgeName(badge.getBadgeName());
        resp.setDescription(badge.getDescription());
        resp.setObtainedAt(badge.getGmtCreate());
        return resp;
    }
}
