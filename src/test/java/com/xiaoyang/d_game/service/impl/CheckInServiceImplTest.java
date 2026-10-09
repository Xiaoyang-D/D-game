package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.dto.CheckInRewardResp;
import com.xiaoyang.d_game.repository.CheckInBitmapRepository;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.CheckInRewardRecovery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckInServiceImplTest {
    private final CheckInBitmapRepository bitmap = mock(CheckInBitmapRepository.class);
    private final CheckInRewardRecovery recovery = mock(CheckInRewardRecovery.class);
    private final CheckInServiceImpl service = new CheckInServiceImpl(bitmap, recovery);

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(1L, "player"), null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void intentIsSavedBeforeRewardAttemptAndRetryRemainsPossible() {
        when(recovery.complete(eq(1L), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("reward transaction failed"))
                .thenReturn(new CheckInRewardResp());
        assertThrows(IllegalStateException.class, service::checkIn);
        var order = inOrder(recovery);
        order.verify(recovery).enqueue(eq(1L), any(LocalDate.class));
        order.verify(recovery).complete(eq(1L), any(LocalDate.class));
        assertNotNull(service.checkIn());
        verify(recovery, times(2)).enqueue(eq(1L), any(LocalDate.class));
    }

    @Test
    void pendingRewardIsExposedEvenWhenBitmapAlreadyShowsCheckedIn() {
        when(bitmap.isCheckedIn(eq(1L), any(LocalDate.class))).thenReturn(true);
        when(recovery.isPending(eq(1L), any(LocalDate.class))).thenReturn(true);
        var status = service.getStatus();
        assertTrue(status.isCheckedInToday());
        assertTrue(status.isRewardPending());
    }

    @Test
    void failureToPersistIntentPreventsRewardAttempt() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(recovery).enqueue(eq(1L), any(LocalDate.class));
        assertThrows(IllegalStateException.class, service::checkIn);
        verify(recovery, never()).complete(anyLong(), any(LocalDate.class));
        verifyNoInteractions(bitmap);
    }
}
