package com.xiaoyang.d_game.security;

import com.xiaoyang.d_game.common.BizException;
import com.xiaoyang.d_game.entity.User;
import com.xiaoyang.d_game.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
/**
 * 从 Authorization 请求头解析 access JWT，并转换为 Spring Security Authentication。
 *
 * <p>每次请求都会重新查询用户状态和角色，确保账号被封禁或角色变更后旧 token 不会继续获得权限。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenRevocationService tokenRevocationService;
    private final UserService userService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    /** 认证成功后把用户身份放入 SecurityContext，再交给后续过滤器处理。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtUtil.parseAccessToken(token);
            if (tokenRevocationService.isRevoked(claims.getId())) {
                throw new BizException(com.xiaoyang.d_game.common.ResultCode.UNAUTHORIZED);
            }
            Long userId = jwtUtil.getUserId(claims);
            User user = userService.getById(userId);
            if (user == null) {
                throw new BizException(com.xiaoyang.d_game.common.ResultCode.UNAUTHORIZED);
            }
            userService.checkUserAvailable(user);
            List<SimpleGrantedAuthority> authorities = userService.listRoleCodes(userId).stream()
                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .toList();
            CurrentUserPrincipal principal = new CurrentUserPrincipal(userId, user.getUsername());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BizException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, null);
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
