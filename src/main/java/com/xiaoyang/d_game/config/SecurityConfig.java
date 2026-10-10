package com.xiaoyang.d_game.config;

import com.xiaoyang.d_game.security.JwtAuthenticationFilter;
import com.xiaoyang.d_game.security.RestAccessDeniedHandler;
import com.xiaoyang.d_game.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
/**
 * Spring Security 全局安全配置。
 *
 * <p>项目采用无状态 Bearer JWT 认证，不创建服务端会话；认证接口和公开读取接口放行，
 * 管理员接口要求 ADMIN 角色，其余接口默认要求登录。</p>
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /** 构建无状态的安全过滤链，并注册 JWT 认证过滤器。 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 关闭 CSRF：当前认证凭据只从 Authorization 请求头读取，不依赖浏览器自动携带 Cookie。
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 复用下面声明的 CORS 配置，先处理跨域预检，再进入认证流程。
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT 自带用户状态，因此不在服务器端创建或保存 HTTP Session。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API 不使用 Spring Security 默认的表单登录、Basic Auth 和请求缓存。
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // 将认证失败和权限不足转换为项目统一的 JSON Result。
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // 按开放接口、管理员接口、公开读取接口和默认受保护接口依次配置访问规则。
                .authorizeHttpRequests(authorize -> authorize
                        // 浏览器跨域预检不携带业务 token，必须允许其先通过。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 注册、登录和刷新 token 是认证入口，不能要求已有 access token。
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/email/code", "/api/v1/auth/password/reset",
                                "/api/v1/auth/migration/verify", "/api/v1/auth/migration/email/code", "/api/v1/auth/migration/bind").permitAll()
                        // API 文档和已经由 MVC 映射的静态文件对外开放。
                        .requestMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**", "/swagger-ui/**", "/files/**").permitAll()
                        // 管理路径除了登录外还必须包含 ADMIN 角色。
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 游戏、帖子列表、搜索和公开用户资料允许匿名读取。
                        .requestMatchers(HttpMethod.GET, "/api/v1/boards/**", "/api/v1/comments/**", "/api/v1/games/**",
                                "/api/v1/posts", "/api/v1/posts/topics", "/api/v1/posts/ranking", "/api/v1/posts/*",
                                "/api/v1/search/**", "/api/v1/users/*/profile", "/api/v1/users/*/favorites").permitAll()
                        // 未被前面明确放行的接口默认要求认证，避免新增接口意外暴露。
                        .anyRequest().authenticated())
                // 在 Spring 默认用户名密码过滤器之前解析项目自己的 JWT。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 将项目配置中的允许来源转换为 Spring Security 使用的 CORS 配置。 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 创建跨域配置对象，后续会注册到所有 URL。
        CorsConfiguration configuration = new CorsConfiguration();
        // 配置项允许用逗号填写多个前端来源，去除空格和空值后再交给 Spring。
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        // 允许项目 REST 接口使用的 HTTP 方法，包括 CORS 预检使用的 OPTIONS。
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // 允许 Authorization、Content-Type 等前端请求头。
        configuration.setAllowedHeaders(List.of("*"));
        // 保留凭据兼容性，实际认证凭据仍只接受 Bearer 请求头。
        configuration.setAllowCredentials(true);
        // 浏览器在 3600 秒内可以复用预检结果，减少 OPTIONS 请求。
        configuration.setMaxAge(3600L);
        // 将同一份 CORS 配置应用到所有资源路径。
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
