package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pulsar 消息接收器接口
 * 提供统一的消息接收抽象
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface PulsarMessageReceiver {

    /**
     * 同步接收单条消息
     *
     * @param topic        主题名称
     * @param subscription 订阅名称
     * @param messageType  消息类型
     * @param <T>          消息类型泛型
     * @return 消息内容
     */
    <T> T receive(String topic, String subscription, Class<T> messageType);

    /**
     * 异步接收单条消息
     *
     * @param topic        主题名称
     * @param subscription 订阅名称
     * @param messageType  消息类型
     * @param <T>          消息类型泛型
     * @return 异步消息内容
     */
    <T> CompletableFuture<T> receiveAsync(String topic, String subscription, Class<T> messageType);

    /**
     * 批量接收消息
     *
     * @param topic        主题名称
     * @param subscription 订阅名称
     * @param messageType  消息类型
     * @param maxMessages  最大消息数量
     * @param <T>          消息类型泛型
     * @return 消息列表
     */
    <T> List<T> receiveBatch(String topic, String subscription, Class<T> messageType, int maxMessages);

    /**
     * 创建消费者
     *
     * @param topic        主题名称
     * @param subscription 订阅名称
     * @return 消费者实例
     */
    Consumer<byte[]> createConsumer(String topic, String subscription);

    /**
     * 确认消息
     *
     * @param message 消息
     */
    void acknowledge(Message<?> message);

    /**
     * 否定确认消息（重新投递）
     *
     * @param message 消息
     */
    void negativeAcknowledge(Message<?> message);

    /**
     * 累积确认消息
     *
     * @param message 消息
     */
    void acknowledgeCumulative(Message<?> message);
}