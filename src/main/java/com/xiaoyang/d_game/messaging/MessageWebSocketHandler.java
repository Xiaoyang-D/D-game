package com.xiaoyang.d_game.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.security.*;
import com.xiaoyang.d_game.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 仅接收认证和心跳帧；业务操作走 HTTP，连接只订阅本人资源变化。 */
@Component @RequiredArgsConstructor @Slf4j
public class MessageWebSocketHandler extends TextWebSocketHandler {
    private static final int MAX_CONNECTIONS = 1024;
    private final JwtUtil jwt;
    private final TokenRevocationService revocations;
    private final UserService users;
    private final RealtimeGateway gateway;
    private final ObjectMapper json;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private static final class Connection {
        // 认证截止时间从建立连接开始计算，心跳重新校验当前账号与令牌状态。
        final WebSocketSession session;
        final long opened = System.currentTimeMillis();
        volatile long lastPing = opened;
        volatile Claims claims;
        volatile Long userId;
        Connection(WebSocketSession session) { this.session = session; }
    }
    @Override public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (connections.size() >= MAX_CONNECTIONS) {
            log.warn("消息连接数量达到上限，sessionId={}，上限={}", session.getId(), MAX_CONNECTIONS);
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        session.setTextMessageSizeLimit(4096);
        connections.put(session.getId(), new Connection(session));
        log.debug("消息连接建立，等待认证，sessionId={}", session.getId());
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Connection state = connections.get(session.getId());
        if (state == null) { return; }
        try {
            var frame = json.readTree(message.getPayload());
            if (state.claims == null) {
                if (!"AUTH".equals(frame.path("type").asText()) || System.currentTimeMillis() - state.opened > 5000) {
                    close(state, "未在五秒内提交认证帧"); return;
                }
                // 令牌只从认证帧读取，不写入 URL、日志或实时推送内容。
                Claims claims = jwt.parseAccessToken(frame.path("token").asText());
                state.userId = jwt.getUserId(claims);
                validate(claims, state.userId);
                state.claims = claims;
                session.sendMessage(new TextMessage("{\"type\":\"READY\"}"));
                gateway.register(state.userId, session);
                log.debug("消息连接认证成功，userId={}，sessionId={}", state.userId, session.getId());
            } else if ("PING".equals(frame.path("type").asText())) {
                validate(state.claims, state.userId);
                state.lastPing = System.currentTimeMillis();
                gateway.changed(state.userId, "PONG", null, null);
            } else { close(state, "收到不支持的消息帧"); }
        } catch (BizException | JsonProcessingException | JwtException | IllegalArgumentException | IllegalStateException exception) {
            // 认证及 JSON 错误的异常消息可能包含原始令牌或帧，仅记录类型，不打印异常正文。
            log.debug("消息连接认证或协议校验失败，userId={}，sessionId={}，异常类型={}",
                    state.userId, session.getId(), exception.getClass().getSimpleName());
            close(state, "认证或协议校验失败");
        } catch (Exception exception) {
            log.warn("处理消息连接发生异常，userId={}，sessionId={}", state.userId, session.getId(), exception);
            close(state, "连接处理异常");
        }
    }
    private void validate(Claims claims, Long userId) {
        if (claims.getExpiration().getTime() <= System.currentTimeMillis() || revocations.isRevoked(claims.getId())) {
            throw new IllegalStateException("连接认证已失效");
        }
        var user = users.getById(userId);
        if (user == null) { throw new IllegalStateException("用户不存在"); }
        users.checkUserAvailable(user);
        jwt.validateAuthVersion(claims, user);
    }
    @Scheduled(fixedDelay = 1000, scheduler = "messageSocketScheduler")
    public void expireUnauthenticated() {
        for (var state : connections.values()) {
            if (state.claims == null && System.currentTimeMillis() - state.opened >= 5000) { close(state, "认证等待超时"); }
        }
    }
    @Scheduled(fixedDelay = 30000, scheduler = "messageSocketScheduler")
    public void validateConnections() {
        for (var state : connections.values()) {
            if (state.claims == null) { continue; }
            try {
                validate(state.claims, state.userId);
                if (System.currentTimeMillis() - state.lastPing > 90000) { close(state, "心跳超时"); }
            } catch (BizException | IllegalStateException exception) {
                log.debug("消息连接认证已失效，userId={}，sessionId={}，异常类型={}",
                        state.userId, state.session.getId(), exception.getClass().getSimpleName());
                close(state, "账号或令牌状态已变化");
            } catch (Exception exception) {
                log.warn("消息连接定期校验失败，userId={}，sessionId={}", state.userId, state.session.getId(), exception);
                close(state, "定期校验异常");
            }
        }
    }
    private void close(Connection state, String reason) {
        connections.remove(state.session.getId());
        gateway.remove(state.userId, state.session.getId());
        log.debug("关闭消息连接，userId={}，sessionId={}，原因={}", state.userId, state.session.getId(), reason);
        try { state.session.close(CloseStatus.POLICY_VIOLATION); }
        catch (Exception exception) { log.debug("关闭消息连接失败, sessionId={}", state.session.getId(), exception); }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var state = connections.remove(session.getId());
        if (state != null) { gateway.remove(state.userId, session.getId()); }
        log.debug("消息连接已断开，sessionId={}，关闭码={}", session.getId(), status.getCode());
    }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) {
        var state = connections.get(session.getId());
        log.warn("消息连接传输失败，sessionId={}", session.getId(), exception);
        if (state != null) { close(state, "传输失败"); }
    }
    @PreDestroy public void shutdown() { connections.values().forEach(state -> close(state, "后端关闭")); }
}
