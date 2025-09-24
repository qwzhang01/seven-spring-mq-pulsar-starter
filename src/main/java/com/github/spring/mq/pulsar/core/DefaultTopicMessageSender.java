package com.github.spring.mq.pulsar.core;

import com.github.spring.mq.pulsar.domain.MsgContext;
import org.apache.pulsar.client.api.MessageId;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Multi-producer Pulsar message sender implementation
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
    public MessageId send(Object message, String msgRoute) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return send(message);
    }

    @Override
    public MessageId send(String key, Object message) {
        validTopic();
        return pulsarMessageSender.send(topic, key, message);
    }

    @Override
    public MessageId send(String key, Object message, String msgRoute) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return send(key, message);
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
    public CompletableFuture<MessageId> sendAsync(Object message, String msgRoute) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return sendAsync(message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String key, Object message) {
        validTopic();
        return pulsarMessageSender.sendAsync(topic, key, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String key, Object message, String msgRoute) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return sendAsync(key, message);
    }

    @Override
    public MessageId sendAfter(Object message, long delay, TimeUnit unit) {
        validTopic();
        return pulsarMessageSender.sendAfter(topic, message, delay, unit);
    }

    @Override
    public MessageId sendAfter(Object message, String msgRoute, long delay, TimeUnit unit) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return sendAfter(message, delay, unit);
    }

    @Override
    public MessageId sendAt(Object message, long timestamp) {
        validTopic();
        return pulsarMessageSender.sendAt(topic, message, timestamp);
    }

    @Override
    public MessageId sendAt(Object message, String msgRoute, long timestamp) {
        MsgContext.setMsgRoute(msgRoute);
        MsgContext.setMultiRoute(true);
        return sendAt(message, timestamp);
    }

    private void validTopic() {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalStateException("Default topic is not configured");
        }
    }
}