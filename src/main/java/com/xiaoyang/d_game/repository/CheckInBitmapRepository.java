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
/**
 * 签到 Bitmap 仓储。
 *
 * <p>每个用户每年使用一个 Redis Bitmap：key 为 {@code checkin:bitmap:{userId}:{year}}，
 * offset 为当年的第几天减一。这样一天只占 1 bit，查询某日是否签到、标记签到和回看连续签到都很轻量。</p>
 */
public class CheckInBitmapRepository {

    /** Redis key 前缀，后面拼接用户 ID 和年份。 */
    private static final String KEY_PREFIX = "checkin:bitmap:";

    /** 签到按中国时区计算自然日，避免服务器时区差异导致日期错位。 */
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /** Bitmap key 保留约 400 天，覆盖跨年连续签到回看。 */
    private static final Duration KEY_TTL = Duration.ofDays(400);

    /** 连续签到最多回看天数，防止 Redis 异常数据导致无限循环。 */
    private static final int MAX_LOOKBACK_DAYS = 400;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 判断用户在指定日期是否已经签到。
     */
    public boolean isCheckedIn(Long userId, LocalDate date) {
        return getBit(userId, date);
    }

    /**
     * 标记用户完成指定日期签到。
     *
     * <p>{@code setBit} 会返回旧值：如果旧值已经是 true，说明用户今天重复签到，应抛出业务异常。</p>
     */
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

    /**
     * 计算从锚点日期向前连续签到的天数。
     *
     * <p>通常锚点是今天或最近一次签到日期。遇到第一天未签到即停止。</p>
     */
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

    /**
     * 查找最近一次签到日期。
     *
     * <p>如果今天已签到，就从今天开始回看；否则从昨天开始回看，避免把“今天未签到”误当作最近日期。</p>
     */
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

    /**
     * 获取按业务时区计算的今天。
     */
    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    /**
     * 读取某个日期对应的 Bitmap 位。
     */
    private boolean getBit(Long userId, LocalDate date) {
        String key = bitmapKey(userId, date.getYear());
        long offset = dayOffset(date);
        Boolean checked = stringRedisTemplate.opsForValue().getBit(key, offset);
        return Boolean.TRUE.equals(checked);
    }

    /**
     * 构造某用户某年的 Bitmap key。
     */
    private String bitmapKey(Long userId, int year) {
        return KEY_PREFIX + userId + ":" + year;
    }

    /**
     * 将日期转换为当年 Bitmap 偏移量，1 月 1 日对应 offset 0。
     */
    private long dayOffset(LocalDate date) {
        return date.getDayOfYear() - 1L;
    }
}
