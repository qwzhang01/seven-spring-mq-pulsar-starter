/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.config.PulsarInterceptorConfiguration;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.domain.MsgContext;
import com.github.spring.mq.pulsar.exception.JacksonException;
import com.github.spring.mq.pulsar.exception.PulsarConsumeInitException;
import com.github.spring.mq.pulsar.exception.PulsarConsumerNotExistException;
import com.github.spring.mq.pulsar.exception.PulsarProducerConfigException;
import com.github.spring.mq.pulsar.exception.PulsarProducerInitException;
import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import com.github.spring.mq.pulsar.listener.DeadLetterListenerContainer;
import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainer;
import com.github.spring.mq.pulsar.tracing.PulsarMessageHeadersPropagator;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.client.impl.MultiTopicsConsumerImpl;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar operations template class
 *
 * <p>This class provides a high-level abstraction for Pulsar operations, including:
 * <ul>
 *   <li>Message sending (sync/async)</li>
 *   <li>Delayed message sending</li>
 *   <li>Consumer management</li>
 *   <li>Message interceptor support</li>
 *   <li>Dead letter queue handling</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public final class PulsarTemplate {

    private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(PulsarTemplate.class);

    private final PulsarClient pulsarClient;
    private final PulsarProperties pulsarProperties;
    private final ObjectMapper objectMapper;
    private final DeadLetterMessageProcessor deadLetterMessageProcessor;
    private final ConcurrentHashMap<String, Producer<byte[]>> producerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<byte[]>> consumerCache = new ConcurrentHashMap<>();
    private final List<DeadLetterListenerContainer> deadLetterListenerContainers = new ArrayList<>();

    private PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry;

    public PulsarTemplate(PulsarClient pulsarClient, PulsarProperties pulsarProperties,
                          ObjectMapper objectMapper,
                          DeadLetterMessageProcessor deadLetterMessageProcessor) {
        this.pulsarClient = pulsarClient;
        this.pulsarProperties = pulsarProperties;
        this.objectMapper = objectMapper;
        this.deadLetterMessageProcessor = deadLetterMessageProcessor;
    }

    public void setInterceptorRegistry(PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry) {
        this.interceptorRegistry = interceptorRegistry;
    }

    /**
     * Send message synchronously
     */
    public MessageId send(String topic, Object message) throws PulsarClientException {
        return send(topic, null, message);
    }

    /**
     * Send message synchronously with key
     */
    public MessageId send(String topic, String key, Object message) throws PulsarClientException {
        // Execute before-send interceptors
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // Interceptor returned null, do not send message
            return null;
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

            // Inject trace context into message headers
            injectTraceContext(messageBuilder);

            messageId = messageBuilder.send();
            return messageId;
        } catch (Exception e) {
            sendException = e;
            throw e;
        } finally {
            // Execute after-send interceptors
            applyAfterSendInterceptors(topic, interceptedMessage, messageId, sendException);
        }
    }

    /**
     * Send delayed message
     *
     * @param topic   topic name
     * @param message message content
     * @param delay   delay time
     * @param unit    time unit
     * @return message ID
     * @throws PulsarClientException if sending fails
     */
    public MessageId sendAfter(String topic, Object message, long delay, TimeUnit unit) throws PulsarClientException {
        return sendAfter(topic, null, message, delay, unit);
    }

    /**
     * Send delayed message with key
     *
     * @param topic   topic name
     * @param key     message key
     * @param message message content
     * @param delay   delay time
     * @param unit    time unit
     * @return message ID
     * @throws PulsarClientException if sending fails
     */
    public MessageId sendAfter(String topic, String key, Object message, long delay, TimeUnit unit) throws PulsarClientException {
        // Execute before-send interceptors
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // Interceptor returned null, do not send message
            return null;
        }

        MessageId messageId = null;
        Exception sendException = null;

        try {
            Producer<byte[]> producer = getOrCreateProducer(topic);
            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage()
                    .value(serialize(interceptedMessage))
                    .deliverAfter(delay, unit);

            if (StringUtils.hasText(key)) {
                messageBuilder.key(key);
            }

            // Inject trace context into message headers
            injectTraceContext(messageBuilder);

            messageId = messageBuilder.send();
            return messageId;
        } catch (Exception e) {
            sendException = e;
            throw e;
        } finally {
            // Execute after-send interceptors
            applyAfterSendInterceptors(topic, interceptedMessage, messageId, sendException);
        }
    }


    /**
     * Send message at specific timestamp
     */
    public MessageId sendAt(String topic, Object message, long timestamp) throws PulsarClientException {
        return sendAt(topic, null, message, timestamp);
    }

    /**
     * Send message at specific timestamp with key
     */
    public MessageId sendAt(String topic, String key, Object message, long timestamp) throws PulsarClientException {
        // Execute before-send interceptors
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // Interceptor returned null, do not send message
            return null;
        }

        MessageId messageId = null;
        Exception sendException = null;

        try {
            Producer<byte[]> producer = getOrCreateProducer(topic);
            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage()
                    .value(serialize(interceptedMessage))
                    .deliverAt(timestamp);

            if (StringUtils.hasText(key)) {
                messageBuilder.key(key);
            }

            // Inject trace context into message headers
            injectTraceContext(messageBuilder);

            messageId = messageBuilder.send();
            return messageId;
        } catch (Exception e) {
            sendException = e;
            throw e;
        } finally {
            // Execute after-send interceptors
            applyAfterSendInterceptors(topic, interceptedMessage, messageId, sendException);
        }
    }


    /**
     * Send message asynchronously
     */
    public CompletableFuture<MessageId> sendAsync(String topic, Object message) {
        return sendAsync(topic, null, message);
    }

    /**
     * Send message asynchronously with key
     */
    public CompletableFuture<MessageId> sendAsync(String topic, String key, Object message) {
        // Execute before-send interceptors
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }

        try {
            Producer<byte[]> producer = getOrCreateProducer(topic);
            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage()
                    .value(serialize(interceptedMessage));

            if (StringUtils.hasText(key)) {
                messageBuilder.key(key);
            }

            // Inject trace context into message headers
            injectTraceContext(messageBuilder);

            return messageBuilder.sendAsync()
                    .whenComplete((messageId, exception) -> {
                        // Execute after-send interceptors
                        applyAfterSendInterceptors(topic, interceptedMessage, messageId, exception);
                    });
        } catch (Exception e) {
            // Execute after-send interceptors
            applyAfterSendInterceptors(topic, message, null, e);
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public Consumer<byte[]> getOrCreateConsumer(String consumerNameAnno,
                                                PulsarProperties.Consumer consumer,
                                                ListenerType listenerType,
                                                Map<String, PulsarListenerContainer> listenerContainers) {

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
                                // Can specify maximum retry count, after 3 retries, messages enter dead letter queue
                                .maxRedeliverCount(consumer.getRetryTime())
                                // Specify retry queue
                                .retryLetterTopic("persistent://" + consumer.getRetryTopic());

                        consumerBuilder// Enable retry strategy
                                .enableRetry(true);
                    }
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(consumer.getDeadTopic())) {
                        // Specify dead letter queue
                        deadLetterPolicyBuilder.deadLetterTopic("persistent://" + consumer.getDeadTopic());
                        buildDeadLetterConsumer(consumer.getDeadTopic(), consumer.getDeadTopicSubscriptionName());
                    }
                    consumerBuilder.deadLetterPolicy(deadLetterPolicyBuilder.build());
                }
                if (ListenerType.EVENT.equals(listenerType)) {
                    consumerBuilder.messageListener(new MessageListener<byte[]>() {
                        @Override
                        public void received(Consumer<byte[]> consumer, Message<byte[]> msg) {
                            buildListener(consumer, msg, listenerContainers);
                        }

                        @Override
                        public void reachedEndOfTopic(Consumer<byte[]> consumer) {
                            MessageListener.super.reachedEndOfTopic(consumer);
                        }
                    });
                }

                return consumerBuilder.subscribe();
            } catch (PulsarClientException e) {
                throw new PulsarConsumeInitException("Failed to create consumer for topic: " + consumer.getTopic(), e);
            }
        });
    }

    /**
     * Build listener
     */
    private void buildListener(Consumer<byte[]> consumer, Message<byte[]> message,
                               Map<String, PulsarListenerContainer> listenerContainers) {
        Map<String, String> topicUrlMap = new HashMap<>();
        pulsarProperties.getConsumerMap().forEach((key, value) -> {
            topicUrlMap.put(value.getTopic(), key);
            topicUrlMap.put(value.getRetryTopic(), key);
        });

        if (consumer instanceof MultiTopicsConsumerImpl) {
            List<String> partitionedTopics = ((MultiTopicsConsumerImpl) consumer).getPartitionedTopics();
            if (partitionedTopics != null && !partitionedTopics.isEmpty()) {
                String topic = partitionedTopics.get(0);
                topic = topic.replace("persistent://", "");
                topic = topicUrlMap.get(topic.trim());
                PulsarListenerContainer container = listenerContainers.get(topic);
                if (container != null) {
                    container.processMessage(consumer, message);
                }
                return;
            }
        }

        String topic = topicUrlMap.get(consumer.getTopic());
        PulsarListenerContainer container = listenerContainers.get(topic);
        if (container != null) {
            container.processMessage(consumer, message);
        }
    }

    /**
     * Build dead letter consumer
     */
    private void buildDeadLetterConsumer(String deadTopic, String subName) {
        try {
            Consumer<byte[]> consumer = createConsumer(deadTopic, subName);
            DeadLetterListenerContainer container = new DeadLetterListenerContainer(consumer, deadLetterMessageProcessor);
            deadLetterListenerContainers.add(container);
            container.start();
        } catch (Exception e) {
            logger.error("Failed to build dead letter queue listener", e);
        }
    }

    /**
     * Create consumer
     */
    private Consumer<byte[]> createConsumer(String topic, String subName) throws PulsarClientException {
        return pulsarClient.newConsumer()
                .topic("persistent://" + topic)
                .subscriptionType(SubscriptionType.Shared)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscriptionName(subName)
                .subscribe();
    }

    /**
     * Get or create producer
     */
    private Producer<byte[]> getOrCreateProducer(String topic) {
        return producerCache.computeIfAbsent(topic, t -> {
            try {
                var producerConfig = getProducer(topic);
                return pulsarClient.newProducer()
                        .topic("persistent://" + t)
                        .sendTimeout((int) producerConfig.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .blockIfQueueFull(producerConfig.isBlockIfQueueFull())
                        .maxPendingMessages(producerConfig.getMaxPendingMessages())
                        .enableBatching(producerConfig.isBatchingEnabled())
                        .batchingMaxMessages(producerConfig.getBatchingMaxMessages())
                        .batchingMaxPublishDelay((int) producerConfig.getBatchingMaxPublishDelay().toMillis(), TimeUnit.MILLISECONDS)
                        .create();
            } catch (PulsarClientException e) {
                throw new PulsarProducerInitException("Failed to create producer for topic: " + t, e);
            }
        });
    }

    /**
     * Get producer configuration
     */
    private PulsarProperties.Producer getProducer(String topic) {
        var producer = pulsarProperties.getProducer();
        var producerMap = pulsarProperties.getProducerMap();

        if (!StringUtils.hasText(topic)) {
            if (producer == null) {
                throw new PulsarProducerConfigException("Failed to get producer config for topic: " + topic);
            }
            return producer;
        }

        if ((producerMap == null || producerMap.isEmpty()) && producer == null) {
            throw new PulsarProducerConfigException("Failed to get producer config for topic: " + topic);
        }
        if (producerMap == null || producerMap.isEmpty()) {
            if (!topic.equals(producer.getTopic())) {
                throw new PulsarProducerConfigException("Failed to get producer config for topic: " + topic);
            }
            return producer;
        }

        for (Map.Entry<String, PulsarProperties.Producer> entry : producerMap.entrySet()) {
            if (topic.equals(entry.getValue().getTopic())) {
                return entry.getValue();
            }
        }
        throw new PulsarProducerConfigException("Failed to get producer config for topic: " + topic);
    }

    /**
     * Serialize object
     */
    private byte[] serialize(Object object) {
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
     * Deserialize object
     */
    public <T> T deserialize(byte[] data, String dataKey, Class<T> clazz) {
        try {
            if (ObjectUtils.isEmpty(dataKey)) {
                // String 类型直接转换
                if (clazz == String.class) {
                    return clazz.cast(new String(data, StandardCharsets.UTF_8));
                }
                // byte[] 类型直接返回
                if (clazz == byte[].class) {
                    return clazz.cast(data);
                }
                // 基础类型的转换
                return deserializePrimitiveType(data, clazz);
            }

            // 处理带 dataKey 的情况
            JsonNode node = objectMapper.readTree(data);
            JsonNode dataTree = node.get(dataKey);
            if (dataTree == null) {
                throw new JacksonException("Data key '" + dataKey + "' not found in message",
                        new IllegalArgumentException("Missing data key: " + dataKey));
            }

            // String 类型
            if (clazz == String.class) {
                if (dataTree.isObject()) {
                    return (T) objectMapper.writeValueAsString(dataTree);
                }
                return (T) dataTree.asText();
            }
            // byte[] 类型
            if (clazz == byte[].class) {
                return clazz.cast(dataTree.asText().getBytes(StandardCharsets.UTF_8));
            }
            // 基础类型和复杂对象
            return objectMapper.treeToValue(dataTree, clazz);
        } catch (JacksonException e) {
            throw e;
        } catch (Exception e) {
            throw new JacksonException("Failed to deserialize object", e);
        }
    }

    /**
     * Deserialize primitive types and wrapper types
     */
    @SuppressWarnings("unchecked")
    private <T> T deserializePrimitiveType(byte[] data, Class<T> clazz) throws Exception {
        String strValue = new String(data, StandardCharsets.UTF_8).trim();

        // Boolean 类型
        if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) Boolean.valueOf(strValue);
        }
        // Integer 类型
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(strValue);
        }
        // Long 类型
        if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(strValue);
        }
        // Double 类型
        if (clazz == Double.class || clazz == double.class) {
            return (T) Double.valueOf(strValue);
        }
        // Float 类型
        if (clazz == Float.class || clazz == float.class) {
            return (T) Float.valueOf(strValue);
        }
        // Short 类型
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf(strValue);
        }
        // Byte 类型
        if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf(strValue);
        }
        // Character 类型
        if (clazz == Character.class || clazz == char.class) {
            if (strValue.length() != 1) {
                throw new IllegalArgumentException("Cannot convert string of length " + strValue.length() + " to char");
            }
        }

        // 其他复杂类型使用 Jackson 反序列化
        return objectMapper.readValue(data, clazz);
    }

    /**
     * Get message consumer processor
     */
    public String deserializeMsgRoute(byte[] data, Map<String, String> businessMap) {
        if (businessMap.size() == 1) {
            return businessMap.keySet().iterator().next();
        }
        try {
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
        } catch (Exception e) {
            throw new PulsarConsumerNotExistException("explain handler mapping exception", e);
        }
    }

    /**
     * Execute before-send interceptors
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
                // Interceptor exceptions should not affect message sending, just log them
                logger.error("Error in beforeSend interceptor: " + e.getMessage(), e);
            }
        }
        return currentMessage;
    }

    /**
     * Execute after-send interceptors
     */
    private void applyAfterSendInterceptors(String topic, Object message, MessageId messageId, Throwable exception) {
        if (interceptorRegistry == null) {
            return;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                interceptor.afterSend(topic, message, messageId, exception);
            } catch (Exception e) {
                // Interceptor exceptions should not affect main flow, just log them
                logger.error("Error in afterSend interceptor: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Execute before-receive interceptors
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
                // Interceptor exceptions should not affect message receiving, just log them
                logger.error("Error in beforeReceive interceptor: {}", e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Execute after-receive interceptors
     */
    public void applyAfterReceiveInterceptors(Message<?> message, Object processedMessage, Exception exception) {
        if (interceptorRegistry == null) {
            return;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                interceptor.afterReceive(message, processedMessage, exception);
            } catch (Exception e) {
                // Interceptor exceptions should not affect main flow, just log them
                logger.error("Error in afterReceive interceptor: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Close resources
     */
    public void close() {
        logger.info("Pulsar Producer closing");
        producerCache.values().forEach(producer -> {
            try {
                producer.close();
            } catch (PulsarClientException e) {
                // Ignore exception during shutdown
            }
        });
        producerCache.clear();
        logger.info("Pulsar Producer closed");

        logger.info("Pulsar consumer closing");
        consumerCache.values().forEach(c -> {
            try {
                c.close();
            } catch (PulsarClientException e) {
                // ignore
            }
        });
        consumerCache.clear();

        logger.info("Pulsar dead letter consumer closing");
        logger.info("Pulsar consumer closed");
        for (DeadLetterListenerContainer container : deadLetterListenerContainers) {
            container.stop();
        }
        deadLetterListenerContainers.clear();
        logger.info("Pulsar dead letter consumer closed");
    }

    /**
     * Inject trace context into message headers
     */
    private void injectTraceContext(TypedMessageBuilder<byte[]> messageBuilder) {
        try {
            String traceId = MsgContext.getTraceId();
            String spanId = MsgContext.getSpanId();

            // Inject trace information
            if (traceId != null && spanId != null) {
                PulsarMessageHeadersPropagator.injectTraceContext(messageBuilder, traceId, spanId, true);
                logger.debug("Injected trace context into message - traceId: {}, spanId: {}", traceId, spanId);
            }

            // Inject multi-tenant information
            boolean multiTenant = MsgContext.isMultiTenant();
            if (multiTenant) {
                String corpKey = MsgContext.getCorpKey();
                String appName = MsgContext.getAppName();
                LocalDateTime time = MsgContext.getTime();
                String msgId = UUID.randomUUID().toString().replaceAll("-", "").toLowerCase(Locale.ROOT);
                PulsarMessageHeadersPropagator.injectCorp(messageBuilder, corpKey, appName, time, msgId);
            }
            // Inject routing information
            boolean multiRoute = MsgContext.isMultiRoute();
            if (multiRoute) {
                PulsarMessageHeadersPropagator.injectMsgRoute(messageBuilder, MsgContext.getMsgRoute());
            }
        } catch (Exception e) {
            logger.warn("Failed to inject trace context into message", e);
        }
    }
}