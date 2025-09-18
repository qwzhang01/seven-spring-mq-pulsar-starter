package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多 producer Pulsar 消息发送器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultTopicMessageSender implements TopicMessageSender {
    private PulsarMessageSender pulsarMessageSender;
    private String topic;

    @Override
    public void setPulsarMessageSender(PulsarMessageSender pulsarMessageSender) {
        this.pulsarMessageSender = pulsarMessageSender;
    }

    @Override
    public void setTopic(String topic) {
        this.topic = topic;
    }


    @Override
    public MessageId send(Object message) {
        validTopic();
        return pulsarMessageSender.send(topic, message);
    }

    @Override
    public MessageId send(String key, Object message) {
        validTopic();
        return pulsarMessageSender.send(topic, key, message);
    }


    @Override
    public CompletableFuture<MessageId> sendAsync(Object message) {
        validTopic();
        String defaultTopic = topic;
        if (!StringUtils.hasText(defaultTopic)) {
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Default topic is not configured"));
            return future;
        }
        return pulsarMessageSender.sendAsync(defaultTopic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String key, Object message) {
        validTopic();
        return pulsarMessageSender.sendAsync(topic, key, message);
    }

    @Override
    public MessageId sendAfter(Object message, long delay, TimeUnit unit) {
        validTopic();
        return pulsarMessageSender.sendAfter(topic, message, delay, unit);
    }

    @Override
    public MessageId sendAt(Object message, long timestamp) {
        validTopic();
        return pulsarMessageSender.sendAt(topic, message, timestamp);
    }

    private void validTopic() {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalStateException("Default topic is not configured");
        }
    }
}