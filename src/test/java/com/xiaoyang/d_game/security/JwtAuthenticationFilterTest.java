package com.xiaoyang.d_game.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyang.d_game.config.JwtProperties;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserService userService;
    private TokenRevocationService revocationService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-long-enough-for-hs256");
        properties.setAccessExpireMs(3_600_000L);
        properties.setRefreshExpireMs(86_400_000L);
        jwtUtil = new JwtUtil(properties);
        userService = Mockito.mock(UserService.class);
        revocationService = Mockito.mock(TokenRevocationService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, revocationService, userService,
                new RestAuthenticationEntryPoint(new ObjectMapper()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessTokenCreatesAuthenticationWithCurrentRoles() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setUsername("admin");
        String token = jwtUtil.generateAccessToken(7L, "admin", List.of("ADMIN"));
        when(userService.getById(7L)).thenReturn(user);
        when(userService.listRoleCodes(7L)).thenReturn(List.of("ADMIN"));
        when(revocationService.isRevoked(any())).thenReturn(false);
        FilterChain chain = (request, response) -> { };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);

        assertEquals(7L, CurrentUser.getUserId());
        assertTrue(CurrentUser.hasRole("ADMIN"));
    }

    @Test
    void refreshTokenIsRejectedByAccessFilter() throws Exception {
        String token = jwtUtil.generateRefreshToken(7L, "admin", List.of("ADMIN"));
        FilterChain chain = Mockito.mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}
