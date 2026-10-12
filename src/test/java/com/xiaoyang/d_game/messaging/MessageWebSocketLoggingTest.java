package com.xiaoyang.d_game.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.LogCapture;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.security.JwtUtil;
import com.xiaoyang.d_game.security.TokenRevocationService;
import com.xiaoyang.d_game.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageWebSocketLoggingTest {
    @Test
    void malformedAuthenticationFrameIsClosedWithoutLoggingOriginalToken() throws Exception {
        verifyRejectedFrame("{\"type\":\"AUTH\",\"token\":\"PRIVATE-TOKEN\",\"broken\":}", false);
    }

    @Test
    void rejectedTokenDoesNotLeakExceptionMessageOrStack() throws Exception {
        verifyRejectedFrame("{\"type\":\"AUTH\",\"token\":\"PRIVATE-TOKEN\"}", true);
    }

    private void verifyRejectedFrame(String frame, boolean invalidToken) throws Exception {
        JwtUtil jwt = mock(JwtUtil.class);
        if (invalidToken) {
            when(jwt.parseAccessToken("PRIVATE-TOKEN"))
                    .thenThrow(new BizException(ResultCode.UNAUTHORIZED, "PRIVATE-TOKEN"));
        }
        var handler = new MessageWebSocketHandler(jwt, mock(TokenRevocationService.class), mock(UserService.class),
                mock(RealtimeGateway.class), new ObjectMapper());
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("test-session");
        try (var logs = new LogCapture(MessageWebSocketHandler.class)) {
            handler.afterConnectionEstablished(session);
            handler.handleTextMessage(session, new TextMessage(frame));
            verify(session).close(CloseStatus.POLICY_VIOLATION);
            assertTrue(logs.messages().contains("认证或协议校验失败"));
            assertFalse(logs.messages().contains("PRIVATE-TOKEN"));
            assertTrue(logs.events().stream().allMatch(event -> event.getThrowableProxy() == null));
        }
    }
}
