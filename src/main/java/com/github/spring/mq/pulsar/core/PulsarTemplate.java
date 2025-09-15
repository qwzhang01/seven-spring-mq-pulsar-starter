package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.config.PulsarInterceptorConfiguration;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarConsumeInitException;
import com.github.spring.mq.pulsar.exception.PulsarConsumerNotExistException;
import com.github.spring.mq.pulsar.exception.PulsarProducerInitException;
import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.*;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 操作模板类
 *
 * @author avinzhang
 */
public final class PulsarTemplate {

    private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(PulsarTemplate.class);

    private final PulsarClient pulsarClient;
    private final PulsarProperties pulsarProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Producer<byte[]>> producerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<byte[]>> consumerCache = new ConcurrentHashMap<>();

    private PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry;

    public PulsarTemplate(PulsarClient pulsarClient, PulsarProperties pulsarProperties, ObjectMapper objectMapper) {
        this.pulsarClient = pulsarClient;
        this.pulsarProperties = pulsarProperties;
        this.objectMapper = objectMapper;
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

    public Consumer<byte[]> getOrCreateConsumer(String consumerNameAnno,
                                                PulsarProperties.Consumer consumer) {

        return consumerCache.computeIfAbsent(consumer.getTopic(), t -> {
            String consumerName = StringUtils.hasText(consumerNameAnno) ? consumerNameAnno :
                    UUID.randomUUID().toString().replace("-", "").toLowerCase();

            try {
                ConsumerBuilder<byte[]> consumerBuilder = pulsarClient.newConsumer()
                        .topic("persistent://" + consumer.getTopic())
                        .subscriptionType(SubscriptionType.valueOf(consumer.getSubscriptionType()))
                        .subscriptionName(StringUtils.hasText(consumer.getSubscriptionName())
                                ? consumer.getSubscriptionName()
                                : pulsarProperties.getConsumer().getSubscriptionName())
                        .subscriptionInitialPosition(SubscriptionInitialPosition.valueOf(consumer.getSubscriptionInitialPosition()))
                        .consumerName(consumerName)
                        .ackTimeout(consumer.getAckTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .receiverQueueSize(consumer.getReceiverQueueSize())
                        .negativeAckRedeliveryDelay(consumer.getNegativeAckRedeliveryDelay(), TimeUnit.MILLISECONDS)
                        .autoAckOldestChunkedMessageOnQueueFull(consumer.isAutoAckOldestChunkedMessageOnQueueFull());

                if (StringUtils.hasText(consumer.getRetryTopic())
                        || StringUtils.hasText(consumer.getDeadTopic())) {
                    DeadLetterPolicy.DeadLetterPolicyBuilder deadLetterPolicyBuilder = DeadLetterPolicy.builder();
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(consumer.getRetryTopic())) {
                        deadLetterPolicyBuilder
                                // 可以指定最大重试次数，最大重试三次后，进入到死信队列
                                .maxRedeliverCount(consumer.getRetryTime())
                                // 指定重试队列
                                .retryLetterTopic(consumer.getRetryTopic());

                        consumerBuilder// 开启重试策略
                                .enableRetry(true);
                    }
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(consumer.getDeadTopic())) {
                        // 指定死信队列
                        deadLetterPolicyBuilder.deadLetterTopic(consumer.getDeadTopic());
                    }
                    consumerBuilder.deadLetterPolicy(deadLetterPolicyBuilder.build());
                }

                return consumerBuilder.subscribe();
            } catch (PulsarClientException e) {
                throw new PulsarConsumeInitException("Failed to create consumer for topic: " + consumer.getTopic(), e);
            }
        });
    }

    /**
     * 创建消费者
     */
    public Consumer<byte[]> createConsumer(String topic, String subscriptionName) throws PulsarClientException {
        ConsumerBuilder<byte[]> consumerBuilder = pulsarClient.newConsumer()
                .topic("persistent://" + topic)
                .subscriptionType(SubscriptionType.valueOf(pulsarProperties.getConsumer().getSubscriptionType()))
                .subscriptionName(subscriptionName)
                .subscriptionInitialPosition(SubscriptionInitialPosition.valueOf(pulsarProperties.getConsumer().getSubscriptionInitialPosition()))
                .ackTimeout(pulsarProperties.getConsumer().getAckTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .receiverQueueSize(pulsarProperties.getConsumer().getReceiverQueueSize())
                .negativeAckRedeliveryDelay(pulsarProperties.getConsumer().getNegativeAckRedeliveryDelay(), TimeUnit.MILLISECONDS)
                .autoAckOldestChunkedMessageOnQueueFull(pulsarProperties.getConsumer().isAutoAckOldestChunkedMessageOnQueueFull());

        if (StringUtils.hasText(pulsarProperties.getConsumer().getRetryTopic())
                || StringUtils.hasText(pulsarProperties.getConsumer().getDeadTopic())) {
            DeadLetterPolicy.DeadLetterPolicyBuilder deadLetterPolicyBuilder = DeadLetterPolicy.builder();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(pulsarProperties.getConsumer().getRetryTopic())) {
                deadLetterPolicyBuilder
                        // 可以指定最大重试次数，最大重试三次后，进入到死信队列
                        .maxRedeliverCount(pulsarProperties.getConsumer().getRetryTime())
                        // 指定重试队列
                        .retryLetterTopic(pulsarProperties.getConsumer().getRetryTopic());

                consumerBuilder// 开启重试策略
                        .enableRetry(true);
            }
            if (org.apache.commons.lang3.StringUtils.isNotBlank(pulsarProperties.getConsumer().getDeadTopic())) {
                // 指定死信队列
                deadLetterPolicyBuilder.deadLetterTopic(pulsarProperties.getConsumer().getDeadTopic());
            }
            consumerBuilder.deadLetterPolicy(deadLetterPolicyBuilder.build());
        }

        return consumerBuilder.subscribe();
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
    public <T> T deserialize(byte[] data, String dataKey, Class<T> clazz) {
        try {
            if (clazz == String.class) {
                return clazz.cast(new String(data));
            } else if (clazz == byte[].class) {
                return clazz.cast(data);
            } else {
                if (!StringUtils.hasText(dataKey)) {
                    return objectMapper.readValue(data, clazz);
                }
                JsonNode node = objectMapper.readTree(data);
                return objectMapper.treeToValue(node.get(dataKey), clazz);
            }
        } catch (Exception e) {
            throw new JacksonException("Failed to deserialize object", e);
        }
    }

    /**
     * 获取消息的消费处理器
     *
     * @param data
     * @param businessMap
     * @return
     */
    public String deserializeBusinessType(byte[] data, Map<String, String> businessMap, Class<?> clazz) {
        if (businessMap.size() == 1) {
            return businessMap.keySet().iterator().next();
        }
        try {
            if (clazz == byte[].class) {
                return "";
            }

            JsonNode jsonNode = objectMapper.readTree(new String(data));
            for (Map.Entry<String, String> entry : businessMap.entrySet()) {
                JsonNode node = jsonNode.get(entry.getValue());
                if (node == null) {
                    continue;
                }
                if (node.isTextual()) {
                    if (node.asText().equals(entry.getKey())) {
                        return entry.getKey();
                    }
                }
            }
            return "";
        } catch (JsonProcessingException e) {
            if (clazz == String.class) {
                return "";
            }
            throw new PulsarConsumerNotExistException("explain handler mapping exception", e);
        } catch (Exception e) {
            throw new PulsarConsumerNotExistException("explain handler mapping exception", e);
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
                logger.error("Error in beforeSend interceptor: " + e.getMessage(), e);
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
                logger.error("Error in afterSend interceptor: " + e.getMessage(), e);
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
                logger.error("Error in beforeReceive interceptor: " + e.getMessage(), e);
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
                logger.error("Error in afterReceive interceptor: " + e.getMessage(), e);
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