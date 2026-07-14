package com.xiaoyang.d_game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
/**
 * Redis 访问配置。
 *
 * <p>项目中 Redis 主要用于通知未读数、签到 Bitmap 等高频或适合缓存的数据。
 * 统一配置序列化方式可以避免不同模块写入的数据格式不一致。</p>
 */
public class RedisConfig {

    /**
     * 通用 RedisTemplate。
     *
     * <p>Key 使用字符串序列化，便于排查和手动运维；Value 使用 Jackson JSON 序列化，
     * 可以存储简单对象而不依赖 Java 原生序列化。</p>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
