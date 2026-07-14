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
/**
 * JWT 鉴权拦截器。
 *
 * <p>拦截器负责三件事：从请求头或 Cookie 解析 access token，把合法令牌转换成 {@link UserContext}，
 * 并根据 {@link RequireLogin}/{@link RequireRole} 注解决定是否强制登录或校验角色。</p>
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            // 静态资源、预检请求等非 Controller 方法不需要做注解鉴权。
            return true;
        }

        RequireLogin requireLogin = getAnnotation(handlerMethod, RequireLogin.class);
        RequireRole requireRole = getAnnotation(handlerMethod, RequireRole.class);

        String token = resolveToken(request);
        if (StringUtils.hasText(token)) {
            try {
                // 有 token 就尝试解析：即使接口不是强制登录，也可以让业务层感知“可选登录态”。
                UserContext context = jwtUtil.toUserContext(token);
                log.debug("解析访问令牌成功, path={}, userId={}, username={}",
                        request.getRequestURI(), context.getContextUserId(), context.getUsername());
                if (requireLogin != null || requireRole != null) {
                    // 强制登录接口会再次查询数据库，确保用户未被删除或封禁，并刷新最新角色。
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
                    // 必须登录的接口解析失败时直接抛出；可选登录接口则忽略坏 token，当作游客处理。
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
                // 角色注解是“任一匹配即可”，例如 ADMIN 或 MODERATOR 二选一。
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
        // Tomcat 线程会复用，请求结束必须清理 ThreadLocal，避免用户上下文串到下一次请求。
        UserContext.clear();
    }

    /**
     * 从请求中解析 access token。
     *
     * <p>优先使用标准 {@code Authorization: Bearer xxx} 请求头；没有请求头时再读取 HttpOnly Cookie，
     * 兼容前端主动带 header 和浏览器自动带 Cookie 两种登录方式。</p>
     */
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

    /**
     * 读取方法或类上的鉴权注解。
     *
     * <p>方法注解优先级高于类注解，便于整个 Controller 默认需要登录，同时对个别方法单独配置角色。</p>
     */
    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> annotationClass) {
        T annotation = handlerMethod.getMethodAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(annotationClass);
    }
}
