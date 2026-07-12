package com.xiaoyang.d_game.security;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.common.ResultCode;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireLogin requireLogin = getAnnotation(handlerMethod, RequireLogin.class);
        RequireRole requireRole = getAnnotation(handlerMethod, RequireRole.class);

        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                UserContext context = jwtUtil.toUserContext(token);
                log.debug("解析访问令牌成功, path={}, userId={}, username={}",
                        request.getRequestURI(), context.getContextUserId(), context.getUsername());
                if (requireLogin != null || requireRole != null) {
                    User user = userService.getById(context.getContextUserId());
                    if (user == null) {
                        log.debug("访问令牌对应用户不存在, path={}, userId={}",
                                request.getRequestURI(), context.getContextUserId());
                        throw new BizException(ResultCode.UNAUTHORIZED);
                    }
                    userService.checkUserAvailable(user);
                    context.setRoles(new java.util.HashSet<>(userService.listRoleCodes(user.getId())));
                    log.debug("登录接口鉴权通过, path={}, userId={}, roles={}",
                            request.getRequestURI(), user.getId(), context.getRoles());
                }
                UserContext.set(context);
            } catch (BizException ex) {
                if (requireLogin != null || requireRole != null) {
                    log.debug("接口鉴权失败, path={}, code={}, message={}",
                            request.getRequestURI(), ex.getCode(), ex.getMessage());
                    throw ex;
                }
            }
        }

        if (requireLogin != null || requireRole != null) {
            UserContext context = UserContext.get();
            if (context == null) {
                log.debug("接口缺少登录态, path={}", request.getRequestURI());
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            if (requireRole != null) {
                boolean matched = Arrays.stream(requireRole.value())
                        .anyMatch(context::hasRole);
                if (!matched) {
                    log.debug("接口角色校验失败, path={}, userId={}, requiredRoles={}, actualRoles={}",
                            request.getRequestURI(), context.getContextUserId(), requireRole.value(), context.getRoles());
                    throw new BizException(ResultCode.FORBIDDEN);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        var cookie = WebUtils.getCookie(request, "access_token");
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }
        return null;
    }

    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> annotationClass) {
        T annotation = handlerMethod.getMethodAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(annotationClass);
    }
}
