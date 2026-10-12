package com.xiaoyang.d_game.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.common.*;
import com.xiaoyang.d_game.config.MessageWebSocketConfig;
import com.xiaoyang.d_game.config.NotificationSchedulingConfig;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.security.*;
import com.xiaoyang.d_game.service.UserService;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真正的随机端口 Tomcat 与 WebSocket 帧；不扫描生产应用、不连接任何数据库或中间件。 */
class MessageWebSocketIntegrationTest {
    @Configuration @EnableScheduling
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,RabbitAutoConfiguration.class,RedisAutoConfiguration.class,SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration.class})
    @Import({MessageWebSocketConfig.class, NotificationSchedulingConfig.class})
    static class ConfigurationForTest {
        @Bean JwtUtil jwt() {
            var jwt=mock(JwtUtil.class);
            var first=Jwts.claims().id("test-a").subject("10").expiration(new Date(System.currentTimeMillis()+60000)).build();
            var second=Jwts.claims().id("test-b").subject("20").expiration(new Date(System.currentTimeMillis()+60000)).build();
            when(jwt.parseAccessToken("test-token-a")).thenReturn(first); when(jwt.getUserId(first)).thenReturn(10L);
            when(jwt.parseAccessToken("test-token-b")).thenReturn(second); when(jwt.getUserId(second)).thenReturn(20L);
            when(jwt.parseAccessToken("bad-token")).thenThrow(new BizException(ResultCode.TOKEN_INVALID));
            return jwt;
        }
        @Bean TokenRevocationService revocations() { return mock(TokenRevocationService.class); }
        @Bean UserService users() { var service=mock(UserService.class); when(service.getById(anyLong())).thenAnswer(call -> { var user=new User(); user.setId(call.getArgument(0)); return user; }); return service; }
        @Bean RealtimeGateway gateway(ObjectMapper json) { return new RealtimeGateway(json); }
        @Bean MessageWebSocketHandler handler(JwtUtil jwt,TokenRevocationService revocations,UserService users,RealtimeGateway gateway,ObjectMapper json) { return new MessageWebSocketHandler(jwt,revocations,users,gateway,json); }
    }
    private static ServletWebServerApplicationContext context;
    private static HttpClient http;
    private final List<WebSocket> sockets=new ArrayList<>();
    @BeforeAll static void start() {
        context=(ServletWebServerApplicationContext)new SpringApplicationBuilder(ConfigurationForTest.class).web(WebApplicationType.SERVLET)
                .run("--server.port=0","--app.cors.allowed-origins=http://localhost","--spring.profiles.active=websocket-isolated-test","--spring.main.banner-mode=off");
        http=HttpClient.newHttpClient();
    }
    @AfterAll static void stop() { if(context!=null)context.close(); if(http!=null)http.close(); }
    @AfterEach void close() { sockets.forEach(WebSocket::abort); reset(context.getBean(TokenRevocationService.class)); }
    private static class Listener implements WebSocket.Listener {
        final BlockingQueue<String> frames=new ArrayBlockingQueue<>(10);
        final BlockingQueue<Integer> closed=new ArrayBlockingQueue<>(1);
        private final StringBuilder text=new StringBuilder();
        @Override public void onOpen(WebSocket socket) { socket.request(1); }
        @Override public CompletionStage<?> onText(WebSocket socket,CharSequence data,boolean last) { text.append(data); if(last){frames.add(text.toString());text.setLength(0);}socket.request(1);return null; }
        @Override public CompletionStage<?> onClose(WebSocket socket,int status,String reason) { closed.add(status);return null; }
    }
    private WebSocket connect(Listener listener) throws Exception {
        WebSocket socket=http.newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:"+context.getWebServer().getPort()+"/api/v1/messages/ws"),listener).get(5,TimeUnit.SECONDS);
        sockets.add(socket);return socket;
    }
    @Test void anonymousCannotSubscribeAndIdleConnectionExpires() throws Exception {
        var blocker=new CountDownLatch(1);
        context.getBean("notificationOutboxScheduler",ThreadPoolTaskScheduler.class).execute(() -> {
            try { blocker.await(8,TimeUnit.SECONDS); } catch(InterruptedException exception) { Thread.currentThread().interrupt(); }
        });
        try {
            var listener=new Listener();connect(listener);
            assertEquals(1008,listener.closed.poll(7,TimeUnit.SECONDS));assertTrue(listener.frames.isEmpty());
        } finally { blocker.countDown(); }
    }
    @Test void invalidTokenAndUnauthenticatedControlAreRejected() throws Exception {
        var listener=new Listener();connect(listener).sendText("{\"type\":\"AUTH\",\"token\":\"bad-token\"}",true).join();assertEquals(1008,listener.closed.poll(3,TimeUnit.SECONDS));
        var other=new Listener();connect(other).sendText("{\"type\":\"PING\"}",true).join();assertEquals(1008,other.closed.poll(3,TimeUnit.SECONDS));
    }
    @Test void authenticatedFramesAreIsolatedAndContainNoMessageBody() throws Exception {
        var first=new Listener();connect(first).sendText("{\"type\":\"AUTH\",\"token\":\"test-token-a\"}",true).join();assertTrue(first.frames.poll(3,TimeUnit.SECONDS).contains("READY"));
        var second=new Listener();connect(second).sendText("{\"type\":\"AUTH\",\"token\":\"test-token-b\"}",true).join();assertTrue(second.frames.poll(3,TimeUnit.SECONDS).contains("READY"));
        context.getBean(RealtimeGateway.class).changed(10L,"MESSAGE",100L,20L);
        var frame=first.frames.poll(3,TimeUnit.SECONDS);assertTrue(frame.contains("100"));assertFalse(frame.contains("content"));assertFalse(frame.contains("token"));assertNull(second.frames.poll(200,TimeUnit.MILLISECONDS));
    }
    @Test void heartbeatWorksAndRevocationClosesExistingConnection() throws Exception {
        var listener=new Listener();var socket=connect(listener);socket.sendText("{\"type\":\"AUTH\",\"token\":\"test-token-a\"}",true).join();assertNotNull(listener.frames.poll(3,TimeUnit.SECONDS));
        socket.sendText("{\"type\":\"PING\"}",true).join();assertTrue(listener.frames.poll(3,TimeUnit.SECONDS).contains("PONG"));
        when(context.getBean(TokenRevocationService.class).isRevoked("test-a")).thenReturn(true);
        context.getBean(MessageWebSocketHandler.class).validateConnections();assertEquals(1008,listener.closed.poll(3,TimeUnit.SECONDS));
    }
    @Test void bannedAccountAndChangedAuthVersionCannotAuthenticate() throws Exception {
        var users=context.getBean(UserService.class); var jwt=context.getBean(JwtUtil.class);
        try {
            doThrow(new BizException(ResultCode.USER_BANNED)).when(users).checkUserAvailable(any(User.class));
            var banned=new Listener();connect(banned).sendText("{\"type\":\"AUTH\",\"token\":\"test-token-a\"}",true).join();assertEquals(1008,banned.closed.poll(3,TimeUnit.SECONDS));
            doNothing().when(users).checkUserAvailable(any(User.class));
            doThrow(new BizException(ResultCode.TOKEN_INVALID)).when(jwt).validateAuthVersion(any(),any(User.class));
            var invalid=new Listener();connect(invalid).sendText("{\"type\":\"AUTH\",\"token\":\"test-token-a\"}",true).join();assertEquals(1008,invalid.closed.poll(3,TimeUnit.SECONDS));
        } finally {
            doNothing().when(users).checkUserAvailable(any(User.class));doNothing().when(jwt).validateAuthVersion(any(),any(User.class));
        }
    }
}
