package com.xiaoyang.d_game.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * OpenAPI/Knife4j 接口文档配置。
 *
 * <p>这里声明 API 标题、版本和 JWT Bearer 鉴权方案，让开发者可以在文档页面直接填入
 * {@code Authorization: Bearer <token>} 调试需要登录的接口。</p>
 */
public class Knife4jConfig {

    /**
     * 构建 OpenAPI 描述对象。
     *
     * @return OpenAPI 元信息和安全方案配置
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("游戏社区平台 API")
                        .description("游戏社区平台后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("D_game")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .schemaRequirement("Authorization", new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}
