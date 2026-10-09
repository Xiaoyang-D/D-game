package com.xiaoyang.d_game.service.impl;

import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.entity.UserFollow;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowRecoveryTest {
    private UserFollowMapper follows;
    private NotificationService notifications;
    private InteractServiceImpl service;

    @BeforeEach
    void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "follow-test"), User.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(1L, "follower"), null, List.of()));
        follows = mock(UserFollowMapper.class);
        when(follows.findActiveFollowForUpdate(anyLong(), anyLong())).thenReturn(null);
        notifications = mock(NotificationService.class);
        UserMapper users = mock(UserMapper.class);
        when(users.selectById(2L)).thenReturn(new User());
        service = new InteractServiceImpl(mock(CommentMapper.class), mock(PostMapper.class), users,
                mock(UserLikeMapper.class), mock(UserFavoriteMapper.class), follows, notifications,
                mock(StringRedisTemplate.class));
    }

    @AfterEach
    void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void refollowRestoresDeletedRelationshipWithoutInsert() {
        when(follows.restoreFollow(1L, 2L)).thenReturn(1);
        service.follow(2L);
        verify(follows, never()).insert(any(UserFollow.class));
        verify(notifications).sendNotification(eq(2L), eq(1L), anyInt(), anyString(), anyString(), isNull(), isNull());
    }

    @Test
    void repeatedFollowIsIdempotent() {
        when(follows.selectOne(any())).thenReturn(new UserFollow());
        service.follow(2L);
        verify(follows, never()).insert(any(UserFollow.class));
        verifyNoInteractions(notifications);
    }

    @Test
    void firstFollowInsertsRelationship() {
        service.follow(2L);
        verify(follows).insert(argThat((UserFollow relation) -> relation.getFollowerId().equals(1L)
                && relation.getFolloweeId().equals(2L)));
        verify(notifications).sendNotification(eq(2L), eq(1L), anyInt(), anyString(), anyString(), isNull(), isNull());
    }

    @Test
    void concurrentFollowConflictDoesNotDuplicateNotification() {
        when(follows.insert(any(UserFollow.class))).thenThrow(new DuplicateKeyException("duplicate pair"));
        when(follows.findActiveFollowForUpdate(1L, 2L)).thenReturn(99L);
        assertDoesNotThrow(() -> service.follow(2L));
        verifyNoInteractions(notifications);
    }

    @Test
    void unrelatedDuplicateKeyIsNotSilentlyIgnored() {
        when(follows.insert(any(UserFollow.class))).thenThrow(new DuplicateKeyException("duplicate id"));
        assertThrows(DuplicateKeyException.class, () -> service.follow(2L));
        verifyNoInteractions(notifications);
    }

    @Test
    void repeatedUnfollowIsIdempotent() {
        assertDoesNotThrow(() -> service.unfollow(2L));
        verify(follows, never()).deleteById(anyLong());
    }
}
