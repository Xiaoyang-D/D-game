package com.xiaoyang.d_game.config;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.*;
/** 启用关联确认和不可路由退回，发件箱不把调用发送当作投递成功。 */
@Configuration
public class NotificationConfirmConfig {
    @Bean public RabbitTemplateCustomizer notificationConfirms() {
        return template -> {
            if (template.getConnectionFactory() instanceof CachingConnectionFactory factory) {
                factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
                factory.setPublisherReturns(true);
            }
            template.setMandatory(true);
        };
    }
}
