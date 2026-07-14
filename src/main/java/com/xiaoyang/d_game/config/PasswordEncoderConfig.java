package com.xiaoyang.d_game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
/**
 * 密码编码器配置。
 *
 * <p>用户密码不直接入库，注册时使用 BCrypt 生成哈希，登录时通过 BCrypt 校验明文密码。
 * BCrypt 自带随机盐和成本因子，比普通摘要算法更适合保存登录密码。</p>
 */
public class PasswordEncoderConfig {

    /**
     * 提供 BCrypt 密码编码器 Bean。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

