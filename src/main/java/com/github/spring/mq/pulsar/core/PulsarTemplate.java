package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.config.PulsarInterceptorConfiguration;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarProducerInitException;
import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import org.apache.pulsar.client.api.*;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 操作模板类
 *
 * @author avinzhang
 */
public final class PulsarTemplate {

    private final PulsarClient pulsarClient;
    private final PulsarProperties pulsarProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Producer<byte[]>> producerCache = new ConcurrentHashMap<>();
    private PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry;

    public PulsarTemplate(PulsarClient pulsarClient, PulsarProperties pulsarProperties) {
        this.pulsarClient = pulsarClient;
        this.pulsarProperties = pulsarProperties;
        this.objectMapper = new ObjectMapper();
    }

    public void setInterceptorRegistry(PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry) {
        this.interceptorRegistry = interceptorRegistry;
    }

    /**
     * 同步发送消息
     */
    public MessageId send(String topic, Object message) throws PulsarClientException {
        return send(topic, null, message);
    }

    /**
     * 同步发送消息（带key）
     */
    public MessageId send(String topic, String key, Object message) throws PulsarClientException {
        // 执行发送前拦截器
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            return null; // 拦截器返回null，不发送消息
        }

        MessageId messageId = null;
        Exception sendException = null;

        try {
            Producer<byte[]> producer = getOrCreateProducer(topic);
            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage()
                    .value(serialize(interceptedMessage));

            if (StringUtils.hasText(key)) {
                messageBuilder.key(key);
            }

            messageId = messageBuilder.send();
            return messageId;
        } catch (Exception e) {
            sendException = e;
            throw e;
        } finally {
            // 执行发送后拦截器
            applyAfterSendInterceptors(topic, interceptedMessage, messageId, sendException);
        }
    }

    /**
     * 异步发送消息
     */
    public CompletableFuture<MessageId> sendAsync(String topic, Object message) {
        return sendAsync(topic, null, message);
    }

    /**
     * 异步发送消息（带key）
     */
    public CompletableFuture<MessageId> sendAsync(String topic, String key, Object message) {
        try {
            // 执行发送前拦截器
            Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
            if (interceptedMessage == null) {
                CompletableFuture<MessageId> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }

            Producer<byte[]> producer = getOrCreateProducer(topic);
            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage()
                    .value(serialize(interceptedMessage));

            if (StringUtils.hasText(key)) {
                messageBuilder.key(key);
            }

            return messageBuilder.sendAsync()
                    .whenComplete((messageId, exception) -> {
                        // 执行发送后拦截器
                        applyAfterSendInterceptors(topic, interceptedMessage, messageId, exception);
                    });
        } catch (Exception e) {
            // 执行发送后拦截器
            applyAfterSendInterceptors(topic, message, null, e);
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 创建消费者
     */
    public Consumer<byte[]> createConsumer(String topic, String subscriptionName) throws PulsarClientException {
        return pulsarClient.newConsumer()
                .topic(topic)
                .subscriptionName(subscriptionName)
                .subscriptionType(SubscriptionType.valueOf(pulsarProperties.getConsumer().getSubscriptionType()))
                .ackTimeout(pulsarProperties.getConsumer().getAckTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .receiverQueueSize(pulsarProperties.getConsumer().getReceiverQueueSize())
                .autoAckOldestChunkedMessageOnQueueFull(pulsarProperties.getConsumer().isAutoAckOldestChunkedMessageOnQueueFull())
                .subscribe();
    }

    /**
     * 获取或创建生产者
     */
    public Producer<byte[]> getOrCreateProducer(String topic) throws PulsarClientException {
        return producerCache.computeIfAbsent(topic, t -> {
            try {
                return pulsarClient.newProducer()
                        .topic(t)
                        .sendTimeout((int) pulsarProperties.getProducer().getSendTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .blockIfQueueFull(pulsarProperties.getProducer().isBlockIfQueueFull())
                        .maxPendingMessages(pulsarProperties.getProducer().getMaxPendingMessages())
                        .enableBatching(pulsarProperties.getProducer().isBatchingEnabled())
                        .batchingMaxMessages(pulsarProperties.getProducer().getBatchingMaxMessages())
                        .batchingMaxPublishDelay((int) pulsarProperties.getProducer().getBatchingMaxPublishDelay().toMillis(), TimeUnit.MILLISECONDS)
                        .create();
            } catch (PulsarClientException e) {
                throw new PulsarProducerInitException("Failed to create producer for topic: " + t, e);
            }
        });
    }

    /**
     * 序列化对象
     */
    public byte[] serialize(Object object) {
        try {
            if (object instanceof String) {
                return ((String) object).getBytes();
            } else if (object instanceof byte[]) {
                return (byte[]) object;
            } else {
                return objectMapper.writeValueAsBytes(object);
            }
        } catch (Exception e) {
            throw new JacksonException("Failed to serialize object", e);
        }
    }

    /**
     * 反序列化对象
     */
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            if (clazz == String.class) {
                return clazz.cast(new String(data));
            } else if (clazz == byte[].class) {
                return clazz.cast(data);
            } else {
                return objectMapper.readValue(data, clazz);
            }
        } catch (Exception e) {
            throw new JacksonException("Failed to deserialize object", e);
        }
    }

    /**
     * 执行发送前拦截器
     */
    private Object applyBeforeSendInterceptors(String topic, Object message) {
        if (interceptorRegistry == null) {
            return message;
        }

        Object currentMessage = message;
        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                currentMessage = interceptor.beforeSend(topic, currentMessage);
                if (currentMessage == null) {
                    break;
                }
            } catch (Exception e) {
                // 拦截器异常不应该影响消息发送，记录日志即可
                System.err.println("Error in beforeSend interceptor: " + e.getMessage());
            }
        }
        return currentMessage;
    }

    /**
     * 执行发送后拦截器
     */
    private void applyAfterSendInterceptors(String topic, Object message, MessageId messageId, Throwable exception) {
        if (interceptorRegistry == null) {
            return;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                interceptor.afterSend(topic, message, messageId, exception);
            } catch (Exception e) {
                // 拦截器异常不应该影响主流程，记录日志即可
                System.err.println("Error in afterSend interceptor: " + e.getMessage());
            }
        }
    }

    /**
     * 执行接收前拦截器
     */
    public boolean applyBeforeReceiveInterceptors(Message<?> message) {
        if (interceptorRegistry == null) {
            return true;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                if (!interceptor.beforeReceive(message)) {
                    return false;
                }
            } catch (Exception e) {
                // 拦截器异常不应该影响消息接收，记录日志即可
                System.err.println("Error in beforeReceive interceptor: " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * 执行接收后拦截器
     */
    public void applyAfterReceiveInterceptors(Message<?> message, Object processedMessage, Exception exception) {
        if (interceptorRegistry == null) {
            return;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                interceptor.afterReceive(message, processedMessage, exception);
            } catch (Exception e) {
                // 拦截器异常不应该影响主流程，记录日志即可
                System.err.println("Error in afterReceive interceptor: " + e.getMessage());
            }
        }
    }

    /**
     * 关闭资源
     */
    public void close() {
        producerCache.values().forEach(producer -> {
            try {
                producer.close();
            } catch (PulsarClientException e) {
                // 忽略关闭异常
            }
        });
        producerCache.clear();
    }
}