package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;

/**
 * 多 producer Pulsar 消息发送器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface MultipleMessageSender {
    void setPulsarTemplate(PulsarTemplate pulsarTemplate);

    void setTopic(String topic);

    MessageId send(Object message);

    MessageId send(String key, Object message);

    CompletableFuture<MessageId> sendAsync(Object message);

    CompletableFuture<MessageId> sendAsync(String key, Object message);

    MessageId sendDelayed(Object message, long delayMillis);
}