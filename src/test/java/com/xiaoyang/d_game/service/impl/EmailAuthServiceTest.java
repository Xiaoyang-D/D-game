package com.xiaoyang.d_game.service.impl;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.config.JwtProperties;
import com.xiaoyang.d_game.dto.*;
import com.xiaoyang.d_game.dto.EmailAuthReq.Purpose;
import com.xiaoyang.d_game.entity.*;
import com.xiaoyang.d_game.manager.EmailCodeManager;
import com.xiaoyang.d_game.mapper.*;
import com.xiaoyang.d_game.security.*;
import com.xiaoyang.d_game.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class EmailAuthServiceTest {
    UserService users; UserMapper mapper; EmailCodeManager codes; PasswordEncoder passwords;
    StringRedisTemplate redis; ValueOperations<String,String> values; JwtUtil jwt; AuthServiceImpl auth;
    SysRoleMapper roles; UserRoleRelMapper relations;
    @BeforeEach @SuppressWarnings("unchecked")
    void setup() {
        users=mock(UserService.class); mapper=mock(UserMapper.class); codes=mock(EmailCodeManager.class);
        passwords=mock(PasswordEncoder.class); redis=mock(StringRedisTemplate.class); values=mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        JwtProperties props=new JwtProperties(); props.setSecret("unit-test-only-signing-key-never-use-in-production-2026");
        props.setAccessExpireMs(60000L); props.setRefreshExpireMs(120000L); jwt=new JwtUtil(props);
        roles=mock(SysRoleMapper.class); relations=mock(UserRoleRelMapper.class);
        auth=new AuthServiceImpl(users, mapper, codes, redis, relations, roles,
                passwords,jwt,props,mock(TokenRevocationService.class));
    }
    User user(boolean verified) {
        User user=new User(); user.setId(7L); user.setUsername("old"); user.setEmail("person@example.com");
        user.setAuthVersion(1); user.setPasswordHash("hash");
        if(verified) user.setEmailVerifiedAt(LocalDateTime.now());
        return user;
    }
    @Test void normalizedEmailLoginIncludesVersion() {
        LoginReq req=new LoginReq(); req.setEmail("  Person@Example.com  "); req.setPassword("password");
        assertEquals("person@example.com",req.getEmail());
        when(users.getOne(any())).thenReturn(user(true)); when(passwords.matches("password","hash")).thenReturn(true);
        when(users.listRoleCodes(7L)).thenReturn(List.of("USER"));
        TokenResp response=auth.login(req);
        assertEquals(1,((Number)jwt.parseToken(response.getAccessToken()).get("authVersion")).intValue());
    }
    @Test void unverifiedEmailCannotLogin() {
        when(users.getOne(any())).thenReturn(user(false));
        LoginReq req=new LoginReq(); req.setEmail("person@example.com"); req.setPassword("password");
        assertThrows(BizException.class,()->auth.login(req));
    }
    @Test void occupiedEmailNeverSendsRegistrationCode() {
        when(mapper.countEmailIncludingDeleted("person@example.com")).thenReturn(1L);
        EmailAuthReq.SendCode req=new EmailAuthReq.SendCode(); req.setEmail("person@example.com"); req.setPurpose(Purpose.REGISTER);
        auth.sendEmailCode(req,"127.0.0.1");
        verify(codes).send("person@example.com",Purpose.REGISTER,"","127.0.0.1",false);
    }
    @Test void resetConsumesCodeAndConditionallyUpdatesVersion() {
        User user=user(true); when(users.getOne(any())).thenReturn(user);
        when(passwords.encode("newpassword")).thenReturn("newhash"); when(mapper.resetPassword(7L,1,"newhash")).thenReturn(1);
        EmailAuthReq.ResetPassword req=new EmailAuthReq.ResetPassword(); req.setEmail("person@example.com"); req.setCode("123456"); req.setNewPassword("newpassword");
        auth.resetPassword(req);
        verify(codes).consume("person@example.com",Purpose.RESET_PASSWORD,"","123456");
        verify(mapper).resetPassword(7L,1,"newhash");
    }
    @Test void invalidCodeCannotChangePassword() {
        doThrow(new BizException(com.xiaoyang.d_game.common.ResultCode.BAD_REQUEST)).when(codes).consume(any(),any(),any(),any());
        EmailAuthReq.ResetPassword req=new EmailAuthReq.ResetPassword(); req.setEmail("person@example.com"); req.setCode("123456"); req.setNewPassword("newpassword");
        assertThrows(BizException.class,()->auth.resetPassword(req)); verifyNoInteractions(mapper);
    }
    @Test void staleRefreshIsRejectedAfterReset() {
        when(users.getById(7L)).thenReturn(user(true));
        RefreshTokenReq req=new RefreshTokenReq(); req.setRefreshToken(jwt.generateRefreshToken(7L,"old",List.of(),0));
        assertThrows(BizException.class,()->auth.refresh(req));
    }
    @Test void migrationCannotRebindVerifiedUser() {
        when(values.get("auth:migration:token")).thenReturn("7:1"); when(users.getById(7L)).thenReturn(user(true));
        EmailAuthReq.BindMigration req=new EmailAuthReq.BindMigration(); req.setMigrationToken("token"); req.setEmail("other@example.com"); req.setCode("123456");
        assertThrows(BizException.class,()->auth.bindMigration(req)); verifyNoInteractions(codes,mapper);
    }
    @Test void migrationRetainsIdentityAndConsumesCredential() {
        User old=user(false), updated=user(true); updated.setAuthVersion(2);
        when(values.get("auth:migration:token")).thenReturn("7:1"); when(users.getById(7L)).thenReturn(old,updated);
        when(mapper.bindEmail(7L,1,"person@example.com")).thenReturn(1); when(users.listRoleCodes(7L)).thenReturn(List.of("ADMIN"));
        EmailAuthReq.BindMigration req=new EmailAuthReq.BindMigration(); req.setMigrationToken("token"); req.setEmail("person@example.com"); req.setCode("123456");
        TokenResp response=auth.bindMigration(req);
        assertEquals("7",jwt.parseToken(response.getAccessToken()).getSubject());
        verify(redis).delete("auth:migration:token");
        verify(codes).consume("person@example.com",Purpose.MIGRATION,"token","123456");
    }
    @Test void expiredMigrationIsRejected() {
        EmailAuthReq.MigrationEmail req=new EmailAuthReq.MigrationEmail(); req.setMigrationToken("expired"); req.setEmail("person@example.com");
        assertThrows(BizException.class,()->auth.sendMigrationCode(req,"127.0.0.1")); verifyNoInteractions(codes);
    }
    @Test void publicCodeCannotUseMigrationPurpose() {
        EmailAuthReq.SendCode req=new EmailAuthReq.SendCode(); req.setPurpose(Purpose.MIGRATION);
        assertThrows(BizException.class,()->auth.sendEmailCode(req,"127.0.0.1")); verifyNoInteractions(codes);
    }

    RegisterReq registration() {
        RegisterReq req=new RegisterReq(); req.setEmail("person@example.com"); req.setCode("123456");
        req.setPassword("password"); req.setNickname("昵称"); return req;
    }
    @Test void registrationGeneratesInternalIdentityAndVerifiedUser() {
        when(passwords.encode("password")).thenReturn("hash");
        when(users.save(any(User.class))).thenAnswer(invocation->{ User created=invocation.getArgument(0); created.setId(9L); return true; });
        SysRole role=new SysRole(); role.setId(2L); when(roles.selectOne(any())).thenReturn(role);
        when(users.listRoleCodes(9L)).thenReturn(List.of("USER"));
        TokenResp result=auth.register(registration());
        var capture=org.mockito.ArgumentCaptor.forClass(User.class); verify(users).save(capture.capture());
        assertTrue(capture.getValue().getUsername().matches("u_[0-9a-f]{32}"));
        assertNotNull(capture.getValue().getEmailVerifiedAt()); assertEquals("hash",capture.getValue().getPasswordHash());
        assertEquals("9",jwt.parseToken(result.getAccessToken()).getSubject());
        verify(relations).insert(any(UserRoleRel.class));
        verify(codes).consume("person@example.com",Purpose.REGISTER,"","123456");
    }
    @Test void concurrentEmailUniqueConflictBecomesBusinessError() {
        when(users.save(any(User.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));
        assertThrows(BizException.class,()->auth.register(registration())); verifyNoInteractions(relations);
    }
    @Test void migrationCompareAndSetRejectsSecondBinding() {
        when(values.get("auth:migration:token")).thenReturn("7:1"); when(users.getById(7L)).thenReturn(user(false));
        when(mapper.bindEmail(7L,1,"person@example.com")).thenReturn(0);
        EmailAuthReq.BindMigration req=new EmailAuthReq.BindMigration(); req.setMigrationToken("token"); req.setEmail("person@example.com"); req.setCode("123456");
        assertThrows(BizException.class,()->auth.bindMigration(req)); verify(redis,never()).delete(anyString());
    }
}
