package com.xiaoyang.d_game.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
/** 邮箱认证配置，可信代理必须为精确 IP。 */
@Getter @Setter @Configuration
@org.springframework.validation.annotation.Validated
@ConfigurationProperties(prefix = "app.email-auth")
public class EmailAuthProperties {
    @jakarta.validation.constraints.Min(1)
    private int codeTtlSeconds = 300;
    @jakarta.validation.constraints.Min(1)
    private int resendSeconds = 60;
    @jakarta.validation.constraints.Min(1)
    private int maxAttempts = 5;
    @jakarta.validation.constraints.Min(1)
    private int emailHourlyLimit = 10;
    @jakarta.validation.constraints.Min(1)
    private int ipHourlyLimit = 30;
    private List<String> trustedProxies = List.of();
}
