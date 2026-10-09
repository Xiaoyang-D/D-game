package com.xiaoyang.d_game.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.entity.CheckInRewardTask;
import com.xiaoyang.d_game.mapper.CheckInRewardTaskMapper;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckInRewardRecoveryTest {
    private final CheckInRewardTaskMapper mapper = mock(CheckInRewardTaskMapper.class);
    private final CheckInBitmapRepository bitmap = mock(CheckInBitmapRepository.class);
    private final GrowthService growth = mock(GrowthService.class);
    private final CheckInRewardRecovery recovery = new CheckInRewardRecovery(mapper, bitmap, growth);
    private final LocalDate date = LocalDate.of(2026, 10, 8);

    @BeforeEach
    void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                CheckInRewardTask.class);
    }

    @Test
    void rewardFailureRetainsTaskAndRetryCompletesIt() {
        when(bitmap.calcStreak(1L, date)).thenReturn(7);
        CheckInRewardResp reward = new CheckInRewardResp();
        reward.setPointsEarned(10);
        when(growth.onCheckInSuccess(1L, date, 7))
                .thenThrow(new IllegalStateException("database unavailable")).thenReturn(reward);

        assertThrows(IllegalStateException.class, () -> recovery.complete(1L, date));
        verify(mapper, never()).delete(any());
        assertSame(reward, recovery.complete(1L, date));
        var order = inOrder(growth, mapper);
        order.verify(growth, times(2)).onCheckInSuccess(1L, date, 7);
        order.verify(mapper).delete(any());
    }

    @Test
    void redisFailureDoesNotIssueRewardsOrRemoveTask() {
        doThrow(new IllegalStateException("redis unavailable")).when(bitmap).markCheckedIn(1L, date);
        assertThrows(IllegalStateException.class, () -> recovery.complete(1L, date));
        verifyNoInteractions(growth);
        verify(mapper, never()).delete(any());
    }

    @Test
    void deletionFailureCanReplayAlreadyCommittedRewards() {
        when(bitmap.calcStreak(1L, date)).thenReturn(1);
        CheckInRewardResp replay = new CheckInRewardResp();
        when(growth.onCheckInSuccess(1L, date, 1)).thenReturn(replay);
        when(mapper.delete(any())).thenThrow(new IllegalStateException("delete failed")).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> recovery.complete(1L, date));
        assertEquals(0, recovery.complete(1L, date).getPointsEarned());
        verify(growth, times(2)).onCheckInSuccess(1L, date, 1);
    }

    @Test
    void concurrentEnqueueUsesUniqueConstraint() {
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(CheckInRewardTask.class))).thenThrow(new DuplicateKeyException("same day"));
        assertDoesNotThrow(() -> recovery.enqueue(1L, date));
        verify(mapper).insert(argThat((CheckInRewardTask task) ->
                task.getUserId().equals(1L) && task.getCheckInDate().equals(date)
                        && task.getNextAttemptAt() != null));
    }

    @Test
    void scheduledRetryUsesOriginalDateAndContinuesAfterFailure() {
        CheckInRewardTask first = task(1L, 1L);
        CheckInRewardTask second = task(2L, 2L);
        when(mapper.selectList(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("first user failure")).when(bitmap).markCheckedIn(1L, date);
        when(bitmap.calcStreak(2L, date)).thenReturn(7);
        when(growth.onCheckInSuccess(2L, date, 7)).thenReturn(new CheckInRewardResp());

        recovery.retryPendingRewards();

        verify(growth).onCheckInSuccess(2L, date, 7);
        verify(mapper).delete(any());
        verify(mapper, times(2)).update(isNull(), any());
    }

    private CheckInRewardTask task(Long id, Long userId) {
        CheckInRewardTask task = new CheckInRewardTask();
        task.setId(id);
        task.setUserId(userId);
        task.setCheckInDate(date);
        return task;
    }
}
