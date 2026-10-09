package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.entity.UserBadge;
import com.xiaoyang.d_game.entity.UserPointLog;
import com.xiaoyang.d_game.mapper.UserBadgeMapper;
import com.xiaoyang.d_game.mapper.UserPointLogMapper;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GrowthServiceImplTest {
    @Test
    void replayOfCommittedRewardDoesNotInsertPointsOrBadgesAgain() {
        UserPointLogMapper points = mock(UserPointLogMapper.class);
        UserBadgeMapper badges = mock(UserBadgeMapper.class);
        GrowthServiceImpl growth = new GrowthServiceImpl(points, badges, mock(CheckInBitmapRepository.class));
        when(points.selectOne(any())).thenReturn(null, new UserPointLog());
        when(badges.selectOne(any())).thenReturn(null, null, new UserBadge(), new UserBadge());
        LocalDate date = LocalDate.of(2026, 10, 8);

        var first = growth.onCheckInSuccess(1L, date, 7);
        var replay = growth.onCheckInSuccess(1L, date, 7);

        assertTrue(first.getPointsEarned() > 0);
        assertEquals(2, first.getNewBadges().size());
        assertEquals(0, replay.getPointsEarned());
        assertTrue(replay.getNewBadges().isEmpty());
        verify(points).insert(any(UserPointLog.class));
        verify(badges, times(2)).insert(any(UserBadge.class));
    }
}
