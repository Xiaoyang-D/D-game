package com.xiaoyang.d_game.service.impl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.dto.*;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class ProfileEmailGuardTest {
    UserServiceImpl service;
    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(7L,"old"),null,List.of()));
        service=mock(UserServiceImpl.class,CALLS_REAL_METHODS);
        User stored=new User(); stored.setId(7L); stored.setPasswordHash("new-password-hash");
        stored.setAuthVersion(2); stored.setEmail("person@example.com"); stored.setEmailVerifiedAt(LocalDateTime.now());
        doReturn(stored).when(service).getRequiredUser(7L);
        doReturn(true).when(service).updateById(any(User.class));
        doReturn(new UserResp()).when(service).toUserResp(any());
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void profileCannotWriteEmailEvenWhenUnchanged() {
        UpdateProfileReq req=new UpdateProfileReq(); req.setEmail("person@example.com");
        assertThrows(BizException.class,()->service.updateProfile(req)); verify(service,never()).updateById(any(User.class));
    }
    @Test void nicknameUpdateNeverWritesStaleAuthenticationFields() {
        UpdateProfileReq req=new UpdateProfileReq(); req.setNickname("new nickname"); service.updateProfile(req);
        ArgumentCaptor<User> capture=ArgumentCaptor.forClass(User.class); verify(service).updateById(capture.capture());
        User patch=capture.getValue(); assertEquals("new nickname",patch.getNickname());
        assertNull(patch.getPasswordHash()); assertNull(patch.getAuthVersion());
        assertNull(patch.getEmail()); assertNull(patch.getEmailVerifiedAt()); assertNull(patch.getStatus());
    }
}
