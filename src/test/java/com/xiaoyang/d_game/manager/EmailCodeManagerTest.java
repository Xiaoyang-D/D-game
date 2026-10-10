package com.xiaoyang.d_game.manager;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.config.EmailAuthProperties;
import com.xiaoyang.d_game.dto.EmailAuthReq.Purpose;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
@SuppressWarnings("unchecked")
class EmailCodeManagerTest {
    StringRedisTemplate redis; AuthMailManager mail; EmailCodeManager codes;
    @BeforeEach void setup() {
        redis=mock(StringRedisTemplate.class); mail=mock(AuthMailManager.class);
        codes=new EmailCodeManager(redis,mail,new EmailAuthProperties());
        ReflectionTestUtils.setField(codes,"secret","unit-test-only-signing-key-never-use-in-production-2026");
    }
    @Test void successfulSendPublishesAfterMailAndDoesNotStorePlainCode() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(1L);
        codes.send("person@example.com",Purpose.REGISTER,"","127.0.0.1",true);
        var order=inOrder(redis,mail);
        order.verify(redis).execute(any(RedisScript.class),anyList(),any(Object[].class));
        ArgumentCaptor<String> code=ArgumentCaptor.forClass(String.class);
        order.verify(mail).send(eq("person@example.com"),code.capture(),eq(300));
        ArgumentCaptor<Object[]> args=ArgumentCaptor.forClass(Object[].class);
        order.verify(redis).execute(any(RedisScript.class),anyList(),args.capture());
        assertTrue(code.getValue().matches("[0-9]{6}"));
        assertTrue(args.getValue()[1].toString().matches("[0-9a-f]{64}"));
        assertNotEquals(code.getValue(),args.getValue()[1]);
    }
    @Test void smtpFailureReleasesReservationWithoutPublishing() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(1L);
        doThrow(new org.springframework.mail.MailSendException("test failure")).when(mail).send(anyString(),anyString(),anyInt());
        assertThrows(BizException.class,()->codes.send("person@example.com",Purpose.REGISTER,"","127.0.0.1",true));
        verify(redis,times(2)).execute(any(RedisScript.class),anyList(),any(Object[].class));
    }
    @Test void ineligibleAddressHasSameReservationButNoMail() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(1L);
        codes.send("person@example.com",Purpose.REGISTER,"","127.0.0.1",false);
        verifyNoInteractions(mail); verify(redis).execute(any(RedisScript.class),anyList(),any(Object[].class));
    }
    @Test void rateLimitDoesNotSend() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenReturn(0L);
        assertThrows(BizException.class,()->codes.send("person@example.com",Purpose.REGISTER,"","127.0.0.1",true));
        verifyNoInteractions(mail);
    }
    @Test void redisFailureNeverSendsOrAcceptsCode() {
        when(redis.execute(any(RedisScript.class),anyList(),any(Object[].class))).thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("test"));
        assertThrows(RuntimeException.class,()->codes.send("person@example.com",Purpose.REGISTER,"","127.0.0.1",true));
        assertThrows(RuntimeException.class,()->codes.consume("person@example.com",Purpose.REGISTER,"","123456"));
        verifyNoInteractions(mail);
    }
}
