package com.xiaoyang.d_game.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 邮箱认证配置，可信代理必须为精确 IP。
 */
@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.email-auth")
public class EmailAuthProperties {
    @Min(1)
    private int codeTtlSeconds = 300;
    @Min(1)
    private int resendSeconds = 60;
    @Min(1)
    private int maxAttempts = 5;
    @Min(1)
    private int emailHourlyLimit = 10;
    @Min(1)
    private int ipHourlyLimit = 30;
    private List<String> trustedProxies = List.of();
}
