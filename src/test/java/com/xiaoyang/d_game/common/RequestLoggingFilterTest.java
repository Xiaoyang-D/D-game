package com.xiaoyang.d_game.common;

import com.xiaoyang.d_game.dto.MessageDtos;
import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestLoggingFilterTest {
    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void generatesServerIdAndNeverLogsSensitiveRequestFields() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v1/conversations/123/messages");
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "CLIENT-ID");
        request.addHeader("Authorization", "Bearer PRIVATE-TOKEN");
        request.addHeader("Cookie", "password=PRIVATE-PASSWORD");
        request.setQueryString("token=PRIVATE-QUERY");
        request.setContent("PRIVATE-MESSAGE-BODY".getBytes(StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        try (var logs = new LogCapture(RequestLoggingFilter.class)) {
            new RequestLoggingFilter().doFilter(request, response, (req, res) ->
                    assertEquals(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER), MDC.get(RequestLoggingFilter.REQUEST_ID)));
            String id = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
            assertNotNull(UUID.fromString(id));
            assertNotEquals("CLIENT-ID", id);
            assertTrue(logs.messages().contains(id));
            assertTrue(logs.messages().contains("uri=/api/v1/conversations/123/messages"));
            assertFalse(logs.messages().contains("PRIVATE-"));
            assertNull(MDC.get(RequestLoggingFilter.REQUEST_ID));
        }
    }

    @Test
    void exceptionRestoresPreviousContextWithoutReportingSuccessfulCompletion() {
        MDC.put(RequestLoggingFilter.REQUEST_ID, "outer-request");
        var response = new MockHttpServletResponse();
        var failure = new ServletException("模拟请求异常");
        try (var logs = new LogCapture(RequestLoggingFilter.class)) {
            assertSame(failure, assertThrows(ServletException.class, () -> new RequestLoggingFilter().doFilter(
                    new MockHttpServletRequest("GET", "/test"), response, (req, res) -> { throw failure; })));
            assertEquals("outer-request", MDC.get(RequestLoggingFilter.REQUEST_ID));
            assertTrue(logs.messages().contains("完成=false"));
        }
    }

    @Test
    void nextRequestGetsAnotherIdAndDoesNotInheritPreviousContext() throws Exception {
        var filter = new RequestLoggingFilter();
        var first = new MockHttpServletResponse();
        var second = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/first"), first, (req, res) -> { });
        filter.doFilter(new MockHttpServletRequest("GET", "/second"), second, (req, res) -> { });
        assertNotEquals(first.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER), second.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER));
        assertNull(MDC.get(RequestLoggingFilter.REQUEST_ID));
    }

    @Test
    void unexpectedFailureIncludesRequestIdAndStackInChineseLog() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TestController()).addFilter(new RequestLoggingFilter())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        try (var logs = new LogCapture(GlobalExceptionHandler.class)) {
            var result = mvc.perform(get("/test/failure")).andExpect(status().isInternalServerError()).andReturn();
            assertTrue(logs.messages().contains(result.getResponse().getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)));
            assertTrue(logs.messages().contains("接口系统异常"));
            assertTrue(logs.messages().contains("uri=/test/failure"));
            assertNotNull(logs.events().getFirst().getThrowableProxy());
        }
    }

    @Test
    void validationFailureLogsNoRejectedBodyOrPasswordValue() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TestController()).addFilter(new RequestLoggingFilter())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        try (var logs = new LogCapture(GlobalExceptionHandler.class)) {
            mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"PRIVATE-MESSAGE-BODY\",\"clientMessageId\":\"PRIVATE-PASSWORD\"}"))
                    .andExpect(status().isBadRequest());
            assertTrue(logs.messages().contains("请求参数校验失败"));
            assertFalse(logs.messages().contains("PRIVATE-"));
            assertNull(logs.events().getFirst().getThrowableProxy());
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/test/failure")
        public Result<Void> failure() {
            throw new IllegalStateException("模拟数据库异常");
        }

        @PostMapping("/test/validation")
        public Result<Void> validation(@Valid @RequestBody MessageDtos.Send request) {
            return Result.success();
        }
    }
}
