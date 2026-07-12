package com.xiaoyang.d_game.repository;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CheckInBitmapRepository {

    private static final String KEY_PREFIX = "checkin:bitmap:";
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final Duration KEY_TTL = Duration.ofDays(400);
    private static final int MAX_LOOKBACK_DAYS = 400;

    private final StringRedisTemplate stringRedisTemplate;

    public boolean isCheckedIn(Long userId, LocalDate date) {
        return getBit(userId, date);
    }

    public void markCheckedIn(Long userId, LocalDate date) {
        try {
            String key = bitmapKey(userId, date.getYear());
            long offset = dayOffset(date);
            Boolean already = stringRedisTemplate.opsForValue().setBit(key, offset, true);
            if (Boolean.TRUE.equals(already)) {
                throw new BizException(ResultCode.ALREADY_CHECKED_IN);
            }
            stringRedisTemplate.expire(key, KEY_TTL);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("签到写入 Redis 失败, userId={}, date={}", userId, date, e);
            throw new BizException(ResultCode.INTERNAL_ERROR);
        }
    }

    public int calcStreak(Long userId, LocalDate anchorDate) {
        try {
            int streak = 0;
            LocalDate date = anchorDate;
            for (int i = 0; i < MAX_LOOKBACK_DAYS; i++) {
                if (!getBit(userId, date)) {
                    break;
                }
                streak++;
                date = date.minusDays(1);
            }
            return streak;
        } catch (Exception e) {
            log.warn("计算连续签到天数失败, userId={}, anchorDate={}", userId, anchorDate, e);
            throw new BizException(ResultCode.INTERNAL_ERROR);
        }
    }

    public LocalDate findLastCheckInDate(Long userId, LocalDate today) {
        try {
            LocalDate start = isCheckedIn(userId, today) ? today : today.minusDays(1);
            LocalDate date = start;
            for (int i = 0; i < MAX_LOOKBACK_DAYS; i++) {
                if (getBit(userId, date)) {
                    return date;
                }
                date = date.minusDays(1);
            }
            return null;
        } catch (Exception e) {
            log.warn("查询最近签到日失败, userId={}, today={}", userId, today, e);
            throw new BizException(ResultCode.INTERNAL_ERROR);
        }
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    private boolean getBit(Long userId, LocalDate date) {
        String key = bitmapKey(userId, date.getYear());
        long offset = dayOffset(date);
        Boolean checked = stringRedisTemplate.opsForValue().getBit(key, offset);
        return Boolean.TRUE.equals(checked);
    }

    private String bitmapKey(Long userId, int year) {
        return KEY_PREFIX + userId + ":" + year;
    }

    private long dayOffset(LocalDate date) {
        return date.getDayOfYear() - 1L;
    }
}
