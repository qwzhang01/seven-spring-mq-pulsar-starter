package com.github.spring.mq.pulsar.core;

import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.exception.PulsarProducerSendException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 默认的 Pulsar 消息发送器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultPulsarMessageSender implements PulsarMessageSender {

    private final PulsarTemplate pulsarTemplate;
    private final PulsarProperties pulsarProperties;

    public DefaultPulsarMessageSender(PulsarTemplate pulsarTemplate, PulsarProperties pulsarProperties) {
        this.pulsarTemplate = pulsarTemplate;
        this.pulsarProperties = pulsarProperties;
    }

    @Override
    public MessageId send(Object message) {
        String defaultTopic = pulsarProperties.getProducer().getTopic();
        if (!StringUtils.hasText(defaultTopic)) {
            throw new IllegalStateException("Default topic is not configured");
        }
        try {
            return pulsarTemplate.send(defaultTopic, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message", e);
        }
    }

    @Override
    public MessageId send(String topic, Object message) {
        try {
            return pulsarTemplate.send(topic, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public MessageId send(String topic, String key, Object message) {
        try {
            return pulsarTemplate.send(topic, key, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(Object message) {
        String defaultTopic = pulsarProperties.getProducer().getTopic();
        if (!StringUtils.hasText(defaultTopic)) {
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Default topic is not configured"));
            return future;
        }
        return pulsarTemplate.sendAsync(defaultTopic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String topic, Object message) {
        return pulsarTemplate.sendAsync(topic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String topic, String key, Object message) {
        return pulsarTemplate.sendAsync(topic, key, message);
    }

    @Override
    public MessageId sendDelayed(String topic, Object message, long delayMillis) {
        // 执行发送前拦截器
        Object interceptedMessage = pulsarTemplate.applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // 拦截器返回null，不发送消息
            return null;
        }
        MessageId messageId = null;
        Exception sendException = null;
        try {
            messageId = pulsarTemplate.getOrCreateProducer(topic)
                    .newMessage()
                    .value(pulsarTemplate.serialize(interceptedMessage))
                    .deliverAfter(delayMillis, TimeUnit.MILLISECONDS)
                    .send();
            return messageId;
        } catch (Exception e) {
            sendException = e;
            throw new PulsarProducerSendException(e);
        } finally {
            pulsarTemplate.applyAfterSendInterceptors(topic, interceptedMessage, messageId, sendException);
        }
    }

    @Override
    public MessageId sendInTransaction(String topic, Object message) {
        // TODO: 实现事务消息发送
        throw new UnsupportedOperationException("Transaction message not implemented yet");
    }
}