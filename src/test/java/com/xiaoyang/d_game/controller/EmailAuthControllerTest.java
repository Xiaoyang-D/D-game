package com.xiaoyang.d_game.controller;
import com.xiaoyang.d_game.config.EmailAuthProperties;
import com.xiaoyang.d_game.dto.EmailAuthReq;
import com.xiaoyang.d_game.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.xiaoyang.d_game.common.GlobalExceptionHandler;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class EmailAuthControllerTest {
    @Test void trustedProxyUsesOverwrittenRealIpAndUntrustedPeerIgnoresIt() {
        AuthService service=mock(AuthService.class); EmailAuthProperties props=new EmailAuthProperties();
        props.setTrustedProxies(List.of("127.0.0.1")); AuthController controller=new AuthController(service,props);
        EmailAuthReq.SendCode req=new EmailAuthReq.SendCode(); req.setEmail("person@example.com"); req.setPurpose(EmailAuthReq.Purpose.REGISTER);
        MockHttpServletRequest request=new MockHttpServletRequest(); request.setRemoteAddr("127.0.0.1"); request.addHeader("X-Real-IP","192.0.2.1");
        controller.sendCode(req,request); verify(service).sendEmailCode(req,"192.0.2.1");
        request.setRemoteAddr("192.0.2.2"); controller.sendCode(req,request); verify(service).sendEmailCode(req,"192.0.2.2");
    }
    @Test void oldUsernameLoginPayloadAndMissingRegistrationCodeAreRejected() throws Exception {
        AuthService service=mock(AuthService.class);
        MockMvc mvc=MockMvcBuilders.standaloneSetup(new AuthController(service,new EmailAuthProperties()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"username\":\"old\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{\"email\":\"person@example.com\",\"password\":\"password\",\"nickname\":\"name\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
