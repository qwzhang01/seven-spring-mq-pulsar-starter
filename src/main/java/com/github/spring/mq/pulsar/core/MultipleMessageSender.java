package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多 producer Pulsar 消息发送器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface MultipleMessageSender {
    /**
     * 注入 PulsarTemplate
     *
     * @param pulsarTemplate
     */
    void setPulsarTemplate(PulsarTemplate pulsarTemplate);

    /**
     * 设置 topic
     *
     * @param topic
     */
    void setTopic(String topic);

    /**
     * 发送消息
     *
     * @param message
     * @return
     */
    MessageId send(Object message);

    /**
     * 发送消息
     *
     * @param key
     * @param message
     * @return
     */
    MessageId send(String key, Object message);

    /**
     * 异步发送消息
     *
     * @param message
     * @return
     */
    CompletableFuture<MessageId> sendAsync(Object message);

    /**
     * 异步发送消息
     *
     * @param key
     * @param message
     * @return
     */
    CompletableFuture<MessageId> sendAsync(String key, Object message);

    /**
     * 发送延时消息
     *
     * @param message
     * @param delay
     * @param unit
     * @return
     */
    MessageId sendAfter(Object message, long delay, TimeUnit unit);

    /**
     * 发送延时消息
     *
     * @param message
     * @param timestamp
     * @return
     */
    MessageId sendAt(Object message, long timestamp);
}