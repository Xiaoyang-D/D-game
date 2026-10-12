package com.xiaoyang.d_game.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
/** 隔离 MQ 重试等待，避免阻塞定时发布与连接认证检查。 */
@Configuration
public class NotificationSchedulingConfig {
    @Bean public ThreadPoolTaskScheduler taskScheduler() {
        return scheduler(1, "business-schedule-");
    }
    @Bean public ThreadPoolTaskScheduler messageSocketScheduler() {
        return scheduler(2, "message-socket-");
    }
    @Bean public ThreadPoolTaskScheduler notificationOutboxScheduler() {
        return scheduler(1, "notification-outbox-");
    }
    private ThreadPoolTaskScheduler scheduler(int size, String prefix) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(size); scheduler.setThreadNamePrefix(prefix);
        scheduler.setAwaitTerminationSeconds(10); scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
