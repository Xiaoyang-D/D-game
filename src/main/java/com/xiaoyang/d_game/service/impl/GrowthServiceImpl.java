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
/**
 * 用户成长体系实现。
 *
 * <p>积分通过流水表累加计算，徽章通过用户徽章表记录获得情况。
 * 积分和徽章都以“先查重、再插入”的方式保证业务幂等，数据库唯一键是最后一道保护。</p>
 */
public class GrowthServiceImpl implements GrowthService {

    private final UserPointLogMapper userPointLogMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final CheckInBitmapRepository checkInBitmapRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 发放积分。
     *
     * <p>source + sourceKey 组成完整幂等键，例如每日签到用 CHECK_IN:2026-07-14，
     * 同一用户同一业务键只会产生一条积分流水。</p>
     */
    public int grantPoints(Long userId, PointSourceEnum source, String sourceKey, int points, String remark) {
        String fullKey = source.getCode() + ":" + sourceKey;
        log.debug("准备发放积分, userId={}, source={}, sourceKey={}, points={}",
                userId, source.getCode(), fullKey, points);
        UserPointLog existing = userPointLogMapper.selectOne(new LambdaQueryWrapper<UserPointLog>()
                .eq(UserPointLog::getUserId, userId)
                .eq(UserPointLog::getSourceKey, fullKey)
                .last("LIMIT 1"));
        if (existing != null) {
            // 已存在流水说明该业务奖励发放过，直接返回 0 保持调用幂等。
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
    /**
     * 发放徽章。
     */
    public boolean grantBadge(Long userId, BadgeCodeEnum badge) {
        log.debug("准备发放徽章, userId={}, badgeCode={}", userId, badge.getCode());
        UserBadge existing = userBadgeMapper.selectOne(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeCode, badge.getCode())
                .last("LIMIT 1"));
        if (existing != null) {
            // 徽章只授予一次，重复触发时告诉调用方“没有新徽章”。
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
    /**
     * 签到成功后的奖励发放。
     *
     * <p>基础签到积分按日期幂等；首次签到徽章和连续 7 天徽章按用户徽章唯一性幂等。</p>
     */
    public CheckInRewardResp onCheckInSuccess(Long userId, LocalDate date, int streakDays) {
        CheckInRewardResp reward = new CheckInRewardResp();
        // 每日签到积分以日期作为业务键，一天最多获得一次。
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
    /**
     * 查询用户成长汇总。
     *
     * <p>总积分实时汇总积分流水，徽章按获得时间排序返回。</p>
     */
    public UserGrowthResp getUserGrowth(Long userId) {
        log.debug("查询用户成长信息, userId={}", userId);
        LocalDate today = CheckInBitmapRepository.today();
        boolean checkedInToday = checkInBitmapRepository.isCheckedIn(userId, today);
        // 未签到时从昨天开始计算连续天数，避免今天的空位把历史连续天数清零。
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

    /**
     * 将用户徽章实体转换成个人中心展示 DTO。
     */
    private BadgeResp toBadgeResp(UserBadge badge) {
        BadgeResp resp = new BadgeResp();
        resp.setBadgeCode(badge.getBadgeCode());
        resp.setBadgeName(badge.getBadgeName());
        resp.setDescription(badge.getDescription());
        resp.setObtainedAt(badge.getGmtCreate());
        return resp;
    }
}
