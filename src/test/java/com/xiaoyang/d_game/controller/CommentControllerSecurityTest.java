package com.xiaoyang.d_game.controller;

import com.xiaoyang.d_game.security.CurrentUserPrincipal;
import com.xiaoyang.d_game.service.InteractService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class TestConfiguration {
        @Bean
        InteractService service() {
            return mock(InteractService.class);
        }

        @Bean
        CommentController controller(InteractService service) {
            return new CommentController(service);
        }
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deletionRequiresLoginAndReturnsTheExistingSuccessEnvelope() {
        try (var context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            CommentController controller = context.getBean(CommentController.class);
            InteractService service = context.getBean(InteractService.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.delete(101L));
            verifyNoInteractions(service);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    new CurrentUserPrincipal(10L, "test"), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            var result = controller.delete(101L);
            assertEquals(0, result.getCode());
            assertNull(result.getData());
            verify(service).deleteComment(101L);
        }
    }

    @Test
    void httpDeleteRouteIsRegisteredAndPreservesSnowflakeIds() throws Exception {
        InteractService service = mock(InteractService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CommentController(service)).build();
        mvc.perform(delete("/api/v1/comments/2062801749048246274"))
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(CommentController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(jsonPath("$.code").value(0));
        verify(service).deleteComment(2062801749048246274L);
    }
}
