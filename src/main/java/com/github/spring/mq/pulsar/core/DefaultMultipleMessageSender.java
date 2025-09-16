package com.github.spring.mq.pulsar.core;

import com.github.spring.mq.pulsar.exception.PulsarProducerSendException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 多 producer Pulsar 消息发送器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultMultipleMessageSender implements MultipleMessageSender {
    private PulsarTemplate pulsarTemplate;
    private String topic;

    @Override
    public void setPulsarTemplate(PulsarTemplate pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    @Override
    public void setTopic(String topic) {
        this.topic = topic;
    }


    @Override
    public MessageId send(Object message) {
        validTopic();
        try {
            return pulsarTemplate.send(topic, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message", e);
        }
    }

    @Override
    public MessageId send(String key, Object message) {
        validTopic();
        try {
            return pulsarTemplate.send(topic, key, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
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
        return pulsarTemplate.sendAsync(defaultTopic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String key, Object message) {
        validTopic();
        return pulsarTemplate.sendAsync(topic, key, message);
    }

    @Override
    public MessageId sendDelayed(Object message, long delayMillis) {
        validTopic();
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

    private void validTopic() {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalStateException("Default topic is not configured");
        }
    }
}