package com.xiaoyang.d_game.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 单实例消息连接注册表；只在数据库事务提交后发送资源变更标识。 */
@Component @RequiredArgsConstructor @Slf4j
public class RealtimeGateway {
    private final ObjectMapper json;
    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    public void register(Long userId, WebSocketSession session) {
        // 与最后一个旧连接移除串行，避免新连接被放入已经脱离注册表的 Map。
        sessions.compute(userId, (id, active) -> {
            var registered = active == null ? new ConcurrentHashMap<String, WebSocketSession>() : active;
            registered.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5000, 65536));
            return registered;
        });
        log.debug("本人实时订阅已注册，userId={}，sessionId={}", userId, session.getId());
    }
    public void remove(Long userId, String sessionId) {
        if (userId != null) {
            sessions.computeIfPresent(userId, (id, active) -> {
                active.remove(sessionId);
                return active.isEmpty() ? null : active;
            });
        }
    }
    public void changed(Long userId, String type, Long conversationId, Long actorId) {
        // 不推送私信正文。客户端按标识重新调用鉴权接口，旧事件也无法恢复撤回后的原文。
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        if (conversationId != null) { event.put("conversationId", conversationId.toString()); }
        if (actorId != null) { event.put("actorId", actorId.toString()); }
        afterCommit(() -> deliver(userId, event));
    }
    /** 事务成功后执行推送或成功日志；回滚时不执行，非事务路径可立即执行。 */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else { action.run(); }
    }
    private void deliver(Long userId, Map<String, Object> event) {
        var active = sessions.get(userId);
        if (active == null) { return; }
        for (var session : active.values()) {
            try {
                if (session.isOpen()) { session.sendMessage(new TextMessage(json.writeValueAsString(event))); }
            } catch (Exception exception) {
                remove(userId, session.getId());
                try { session.close(CloseStatus.SERVER_ERROR); } catch (Exception ignored) {
                    log.debug("已断开的消息连接无法关闭，sessionId={}", session.getId(), ignored);
                }
                // 推送失败只影响实时提示；持久化消息仍可通过客户端轮询补取。
                log.warn("消息实时提示失败，userId={}，sessionId={}，type={}", userId, session.getId(), event.get("type"), exception);
            }
        }
    }
}
