package com.xiaoyang.d_game.config;

import com.xiaoyang.d_game.messaging.MessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

import java.util.Arrays;

/** 使用与 HTTP 相同的来源白名单，令牌通过连接后的 AUTH 帧提交。 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MessageWebSocketConfig implements WebSocketConfigurer {
    private final MessageWebSocketHandler handler;
    @Value("${app.cors.allowed-origins}")
    private String origins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/v1/messages/ws").setAllowedOrigins(
                Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new));
    }
}
