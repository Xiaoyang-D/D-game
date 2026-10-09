package com.xiaoyang.d_game.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigCorsTest {
    private static final String LOCAL_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173";

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173"})
    void localLoginPostReachesNextFilter(String origin) throws Exception {
        MockHttpServletRequest request = request("POST", origin);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reached = new AtomicBoolean();

        filter(LOCAL_ORIGINS).doFilter(request, response, (req, resp) -> reached.set(true));

        assertTrue(reached.get());
        assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173"})
    void localLoginPreflightIsAccepted(String origin) throws Exception {
        MockHttpServletRequest request = request("OPTIONS", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type,authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(LOCAL_ORIGINS).doFilter(request, response, (req, resp) -> fail("预检无需进入登录接口"));

        assertEquals(200, response.getStatus());
        assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void unlistedOriginIsStillRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter(LOCAL_ORIGINS).doFilter(request("POST", "https://untrusted.example"), response,
                (req, resp) -> fail("未授权来源不应进入登录接口"));
        assertEquals(403, response.getStatus());
        assertEquals("Invalid CORS request", response.getContentAsString());
    }

    @Test
    void explicitProductionWhitelistDoesNotAllowLocalOrigins() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter("https://game.example").doFilter(request("POST", "http://127.0.0.1:5173"), response,
                (req, resp) -> fail("生产白名单不应自动放行本地来源"));
        assertEquals(403, response.getStatus());
    }

    private CorsFilter filter(String origins) {
        SecurityConfig config = new SecurityConfig(null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        return new CorsFilter(config.corsConfigurationSource());
    }

    private MockHttpServletRequest request(String method, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/auth/login");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        request.addHeader("Origin", origin);
        return request;
    }
}
