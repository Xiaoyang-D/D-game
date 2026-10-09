package com.xiaoyang.d_game.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckInBitmapRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void alreadySetBitDoesNotBlockRewardRecovery() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setBit(anyString(), anyLong(), eq(true))).thenReturn(true);
        CheckInBitmapRepository repository = new CheckInBitmapRepository(redis);

        assertDoesNotThrow(() -> repository.markCheckedIn(1L, LocalDate.of(2026, 10, 8)));
        verify(redis).expire(eq("checkin:bitmap:1:2026"), any(Duration.class));
    }
}
