package com.xiaoyang.d_game.config;

import com.xiaoyang.d_game.security.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
/**
 * Web MVC 全局配置。
 *
 * <p>负责 CORS、鉴权拦截器和本地上传文件静态映射。这里的配置直接影响浏览器请求能否跨域携带 Cookie、
 * API 是否进入 JWT 校验，以及上传后的图片能否通过 URL 访问。</p>
 */
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    /**
     * 本地文件上传目录，默认 {@code ./uploads}。
     */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 允许跨域访问的前端源，多个源使用英文逗号分隔。
     */
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 配置浏览器跨域策略。
     *
     * <p>因为登录态可以通过 HttpOnly Cookie 传递，所以 {@code allowCredentials(true)} 必须和精确 origin 配合使用，
     * 不能使用通配符 {@code *}。</p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册 JWT 鉴权拦截器。
     *
     * <p>所有 {@code /api/**} 请求都会进入拦截器；认证接口、接口文档和静态文件不需要登录，因此显式排除。</p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/files/**"
                );
    }

    /**
     * 把上传目录映射成静态资源。
     *
     * <p>上传服务保存文件后，前端可以通过 {@code /files/**} 访问这些文件。
     * 生产环境如果使用 Nginx 或对象存储托管静态文件，可以调整这里或直接让网关接管。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
