package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 默认的 Pulsar 消息接收器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultPulsarMessageReceiver implements PulsarMessageReceiver {

    private final PulsarTemplate pulsarTemplate;

    public DefaultPulsarMessageReceiver(PulsarTemplate pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    @Override
    public <T> T receive(String topic, String subscription, Class<T> messageType) {
        T result = null;
        Exception receiveException = null;
        Message<byte[]> message = null;

        try {
            Consumer<byte[]> consumer = pulsarTemplate.createConsumer(topic, subscription);
            message = consumer.receive(30, TimeUnit.SECONDS);
            if (message != null) {
                // 执行接收前拦截器
                if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                    consumer.acknowledge(message);
                    return null;
                }

                result = pulsarTemplate.deserialize(message.getData(), "", messageType);
                consumer.acknowledge(message);
                return result;
            }
            return null;
        } catch (Exception e) {
            receiveException = e;
            throw new RuntimeException("Failed to receive message", e);
        } finally {
            // 执行接收后拦截器
            if (message != null) {
                pulsarTemplate.applyAfterReceiveInterceptors(message, result, receiveException);
            }
        }
    }

    @Override
    public <T> CompletableFuture<T> receiveAsync(String topic, String subscription, Class<T> messageType) {
        try {
            Consumer<byte[]> consumer = pulsarTemplate.createConsumer(topic, subscription);
            return consumer.receiveAsync()
                    .thenApply(message -> {
                        T result = null;
                        Exception processException = null;

                        try {
                            // 执行接收前拦截器
                            if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                                try {
                                    consumer.acknowledge(message);
                                } catch (PulsarClientException e) {
                                    throw new RuntimeException("Failed to acknowledge message", e);
                                }
                                return null;
                            }

                            result = pulsarTemplate.deserialize(message.getData(), "", messageType);
                            consumer.acknowledge(message);
                            return result;
                        } catch (Exception e) {
                            processException = e;
                            throw new RuntimeException("Failed to acknowledge message", e);
                        } finally {
                            // 执行接收后拦截器
                            pulsarTemplate.applyAfterReceiveInterceptors(message, result, processException);
                        }
                    });
        } catch (PulsarClientException e) {
            CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    @Override
    public <T> List<T> receiveBatch(String topic, String subscription, Class<T> messageType, int maxMessages) {
        try {
            Consumer<byte[]> consumer = pulsarTemplate.createConsumer(topic, subscription);
            List<T> results = new ArrayList<>();

            for (int i = 0; i < maxMessages; i++) {
                Message<byte[]> message = consumer.receive(1, TimeUnit.SECONDS);
                if (message != null) {
                    T result = null;
                    Exception processException = null;

                    try {
                        // 执行接收前拦截器
                        if (!pulsarTemplate.applyBeforeReceiveInterceptors(message)) {
                            consumer.acknowledge(message);
                            continue;
                        }

                        result = pulsarTemplate.deserialize(message.getData(), "", messageType);
                        results.add(result);
                        consumer.acknowledge(message);
                    } catch (Exception e) {
                        processException = e;
                        throw e;
                    } finally {
                        // 执行接收后拦截器
                        pulsarTemplate.applyAfterReceiveInterceptors(message, result, processException);
                    }
                } else {
                    break;
                }
            }

            return results;
        } catch (PulsarClientException e) {
            throw new RuntimeException("Failed to receive batch messages", e);
        }
    }

    @Override
    public Consumer<byte[]> createConsumer(String topic, String subscription) {
        try {
            return pulsarTemplate.createConsumer(topic, subscription);
        } catch (PulsarClientException e) {
            throw new RuntimeException("Failed to create consumer", e);
        }
    }

    @Override
    public void acknowledge(Message<?> message) {
        // 注意：这里需要通过消费者来确认消息，而不是通过消息本身
        // 实际使用时需要保存消费者引用
        throw new UnsupportedOperationException("Please use consumer.acknowledge(message) directly");
    }

    @Override
    public void negativeAcknowledge(Message<?> message) {
        // 注意：这里需要通过消费者来否定确认消息
        throw new UnsupportedOperationException("Please use consumer.negativeAcknowledge(message) directly");
    }

    @Override
    public void acknowledgeCumulative(Message<?> message) {
        // 注意：这里需要通过消费者来累积确认消息
        throw new UnsupportedOperationException("Please use consumer.acknowledgeCumulative(message) directly");
    }
}