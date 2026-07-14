/**
 * 站内通知消息包。
 *
 * <p>通知事件从业务线程发布到 RabbitMQ，再由消费者并发写入通知表。
 * 消费失败的消息会进入死信队列，避免一直阻塞主队列。</p>
 */
package com.xiaoyang.d_game.messaging.notification;
