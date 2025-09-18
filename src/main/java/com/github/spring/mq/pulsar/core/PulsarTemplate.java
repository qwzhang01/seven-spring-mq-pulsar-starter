package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.config.PulsarInterceptorConfiguration;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.exception.*;
import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import com.github.spring.mq.pulsar.listener.DeadLetterListenerContainer;
import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainer;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.MultiTopicsConsumerImpl;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
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
            // 拦截器返回null，不发送消息
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
     * 发送延时消息
     *
     * @param topic
     * @param message
     * @param delay
     * @param unit
     * @return
     * @throws PulsarClientException
     */
    public MessageId sendAfter(String topic, Object message, long delay, TimeUnit unit) throws PulsarClientException {
        return sendAfter(topic, null, message, delay, unit);
    }

    /**
     * 发送延时消息
     *
     * @param topic
     * @param key
     * @param message
     * @param delay
     * @param unit
     * @return
     * @throws PulsarClientException
     */
    public MessageId sendAfter(String topic, String key, Object message, long delay, TimeUnit unit) throws PulsarClientException {
        // 执行发送前拦截器
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // 拦截器返回null，不发送消息
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
     * 发送延时消息
     */
    public MessageId sendAt(String topic, Object message, long timestamp) throws PulsarClientException {
        return sendAt(topic, null, message, timestamp);
    }

    /**
     * 发送延时消息
     */
    public MessageId sendAt(String topic, String key, Object message, long timestamp) throws PulsarClientException {
        // 执行发送前拦截器
        Object interceptedMessage = applyBeforeSendInterceptors(topic, message);
        if (interceptedMessage == null) {
            // 拦截器返回null，不发送消息
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

    /*** 异步发送消息（带key） */
    public CompletableFuture<MessageId> sendAsync(String topic, String key, Object message) {
        // 执行发送前拦截器
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
                                // 可以指定最大重试次数，最大重试三次后，进入到死信队列
                                .maxRedeliverCount(consumer.getRetryTime())
                                // 指定重试队列
                                .retryLetterTopic("persistent://" + consumer.getRetryTopic());

                        consumerBuilder// 开启重试策略
                                .enableRetry(true);
                    }
                    if (org.apache.commons.lang3.StringUtils.isNotBlank(consumer.getDeadTopic())) {
                        // 指定死信队列
                        deadLetterPolicyBuilder.deadLetterTopic("persistent://" + consumer.getDeadTopic());
                        buildDeadLetterConsumer(consumer.getDeadTopic());
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

    /*** 构建监听器 */
    private void buildListener(Consumer<byte[]> consumer, Message<byte[]> message,
                               Map<String, PulsarListenerContainer> listenerContainers) {
        Map<String, String> topicUrlMap = new HashMap<>();
        for (Map.Entry<String, PulsarProperties.Consumer> entry : pulsarProperties.getConsumerMap().entrySet()) {
            topicUrlMap.put(entry.getValue().getTopic(), entry.getKey());
            topicUrlMap.put(entry.getValue().getRetryTopic(), entry.getKey());
        }

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

    /*** 构建死信消费者 */
    private void buildDeadLetterConsumer(String deadTopic) {
        try {
            Consumer<byte[]> consumer = createConsumer(deadTopic);
            DeadLetterListenerContainer container = new DeadLetterListenerContainer(consumer, deadLetterMessageProcessor);
            deadLetterListenerContainers.add(container);
            container.start();
        } catch (Exception e) {
            logger.error("构建死信队列监听器异常", e);
        }
    }

    /*** 创建消费者 */
    private Consumer<byte[]> createConsumer(String topic) throws PulsarClientException {
        return pulsarClient.newConsumer()
                .topic("persistent://" + topic)
                .subscriptionType(SubscriptionType.Shared)
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscribe();
    }

    /*** 获取或创建生产者 */
    private Producer<byte[]> getOrCreateProducer(String topic) {
        return producerCache.computeIfAbsent(topic, t -> {
            try {
                PulsarProperties.Producer producerConfig = getProducer(topic);
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

    /*** 获取生产者配置 */
    private PulsarProperties.Producer getProducer(String topic) {
        PulsarProperties.Producer producer = pulsarProperties.getProducer();
        Map<String, PulsarProperties.Producer> producerMap = pulsarProperties.getProducerMap();

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

    /*** 序列化对象 */
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

    /*** 反序列化对象 */
    public <T> T deserialize(byte[] data, String dataKey, Class<T> clazz) {
        try {
            if (!StringUtils.hasText(dataKey)) {
                if (clazz == String.class) {
                    return clazz.cast(new String(data));
                }
                if (clazz == byte[].class) {
                    return clazz.cast(data);
                }
                return objectMapper.readValue(data, clazz);
            }
            JsonNode node = objectMapper.readTree(data);
            JsonNode dataTree = node.get(dataKey);
            String text = dataTree.asText();
            if (clazz == String.class) {
                return clazz.cast(text);
            }
            if (clazz == byte[].class) {
                return clazz.cast(text.getBytes(StandardCharsets.UTF_8));
            }
            return objectMapper.readValue(text, clazz);
        } catch (Exception e) {
            throw new JacksonException("Failed to deserialize object", e);
        }
    }

    /*** 获取消息的消费处理器 */
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

    /*** 执行发送前拦截器 */
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

    /*** 执行发送后拦截器 */
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

    /*** 执行接收前拦截器 */
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
                logger.error("Error in beforeReceive interceptor: {}", e.getMessage(), e);
            }
        }
        return true;
    }

    /*** 执行接收后拦截器 */
    public void applyAfterReceiveInterceptors(Message<?> message, Object processedMessage, Exception exception) {
        if (interceptorRegistry == null) {
            return;
        }

        for (PulsarMessageInterceptor interceptor : interceptorRegistry.interceptors()) {
            try {
                interceptor.afterReceive(message, processedMessage, exception);
            } catch (Exception e) {
                // 拦截器异常不应该影响主流程，记录日志即可
                logger.error("Error in afterReceive interceptor: {}", e.getMessage(), e);
            }
        }
    }

    /*** 关闭资源 */
    public void close() {
        logger.info("Pulsar Producer closing");
        producerCache.values().forEach(producer -> {
            try {
                producer.close();
            } catch (PulsarClientException e) {
                // ignore
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
}