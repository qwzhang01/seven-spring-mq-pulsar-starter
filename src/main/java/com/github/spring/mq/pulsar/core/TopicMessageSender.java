package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多 producer Pulsar 消息发送器实现
 * <p>
 * 基于 topic 的消息发送器
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface TopicMessageSender {
    /**
     * 注入 PulsarTemplate
     */
    void setPulsarMessageSender(PulsarMessageSender pulsarMessageSender);

    /**
     * 设置 topic
     */
    void setTopic(String topic);

    /**
     * 发送消息
     */
    MessageId send(Object message);

    /**
     * 发送消息
     */
    MessageId send(String key, Object message);

    /**
     * 异步发送消息
     */
    CompletableFuture<MessageId> sendAsync(Object message);

    /**
     * 异步发送消息
     */
    CompletableFuture<MessageId> sendAsync(String key, Object message);

    /**
     * 发送延时消息
     */
    MessageId sendAfter(Object message, long delay, TimeUnit unit);

    /**
     * 发送延时消息
     */
    MessageId sendAt(Object message, long timestamp);
}