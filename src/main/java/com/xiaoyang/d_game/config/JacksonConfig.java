package com.xiaoyang.d_game.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花 ID 超出 JS Number 安全整数范围（2^53-1），
 * 将 Long 序列化为字符串，避免前端精度丢失。
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义 Jackson 序列化规则。
     *
     * <p>只追加 Long/long 类型转字符串的规则，不替换 Spring Boot 默认配置，
     * 这样 LocalDateTime、LocalDate 等 Java Time 类型仍然按框架默认模块处理。</p>
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            // 使用 serializerByType 追加序列化器，不覆盖 Spring Boot 默认的 JavaTimeModule
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
        };
    }
}
