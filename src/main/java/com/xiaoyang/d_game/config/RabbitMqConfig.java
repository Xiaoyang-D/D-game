package com.xiaoyang.d_game.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * RabbitMQ 消息队列配置。
 *
 * <p>当前先接入“异步站内通知”场景：业务线程只负责投递通知事件，消费者从队列中并发处理，
 * 写入通知表并更新未读数。队列配置了死信交换机，消费失败且不重新入队的消息会进入死信队列，便于后台排查。</p>
 */
@EnableRabbit
@Configuration
public class RabbitMqConfig {

    /** 业务事件交换机，当前用于通知事件，后续也可扩展签到、审核等事件。 */
    public static final String EVENT_EXCHANGE = "dgame.event.exchange";

    /** 死信交换机，消费失败的消息会被路由到这里。 */
    public static final String DEAD_LETTER_EXCHANGE = "dgame.dead-letter.exchange";

    /** 通知事件队列。 */
    public static final String NOTIFICATION_QUEUE = "dgame.notification.queue";

    /** 通知事件死信队列。 */
    public static final String NOTIFICATION_DEAD_LETTER_QUEUE = "dgame.notification.dlq";

    /** 通知事件路由键。 */
    public static final String NOTIFICATION_ROUTING_KEY = "notification.created";

    /** 通知死信路由键。 */
    public static final String NOTIFICATION_DEAD_LETTER_ROUTING_KEY = "notification.created.dead";

    /**
     * 业务事件交换机。
     */
    @Bean
    public DirectExchange dgameEventExchange() {
        return new DirectExchange(EVENT_EXCHANGE, true, false);
    }

    /**
     * 死信交换机。
     */
    @Bean
    public DirectExchange dgameDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * 通知事件队列。
     *
     * <p>队列声明了 x-dead-letter-exchange 和 x-dead-letter-routing-key，
     * 当消费者抛出异常且容器不重新入队时，RabbitMQ 会自动把消息转入通知死信队列。</p>
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(NOTIFICATION_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 通知死信队列。
     */
    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DEAD_LETTER_QUEUE).build();
    }

    /**
     * 绑定通知队列到业务事件交换机。
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange dgameEventExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(dgameEventExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * 绑定通知死信队列到死信交换机。
     */
    @Bean
    public Binding notificationDeadLetterBinding(Queue notificationDeadLetterQueue,
                                                DirectExchange dgameDeadLetterExchange) {
        return BindingBuilder.bind(notificationDeadLetterQueue)
                .to(dgameDeadLetterExchange)
                .with(NOTIFICATION_DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器。
     *
     * <p>发布端和消费端共用该转换器，让 RabbitMQ 消息体以 JSON 形式保存，便于管理后台查看和排查。</p>
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 通知消费者线程池。
     *
     * <p>这里显式使用 Java 线程池承载消费者并发，线程名带有 notify-consumer 前缀，
     * 便于通过日志和线程 dump 定位通知消费任务。</p>
     */
    @Bean
    public ThreadPoolTaskExecutor notificationConsumerTaskExecutor(
            @Value("${dgame.rabbitmq.notification.executor.core-size:4}") int coreSize,
            @Value("${dgame.rabbitmq.notification.executor.max-size:8}") int maxSize,
            @Value("${dgame.rabbitmq.notification.executor.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("notify-consumer-");
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }

    /**
     * 通知消费者监听容器工厂。
     *
     * <p>{@code defaultRequeueRejected(false)} 是进入死信队列的关键：消费方法抛出异常后，
     * 消息会被拒绝且不重新入队，再由队列的死信配置路由到 DLQ。</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory notificationRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            ThreadPoolTaskExecutor notificationConsumerTaskExecutor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup,
            @Value("${dgame.rabbitmq.notification.listener.concurrency:4}") int concurrency,
            @Value("${dgame.rabbitmq.notification.listener.max-concurrency:8}") int maxConcurrency,
            @Value("${dgame.rabbitmq.notification.listener.prefetch:10}") int prefetchCount) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setTaskExecutor(notificationConsumerTaskExecutor);
        factory.setAutoStartup(autoStartup);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setPrefetchCount(prefetchCount);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
