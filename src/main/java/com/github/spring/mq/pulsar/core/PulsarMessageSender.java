package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;

/**
 * Pulsar 消息发送器接口
 * 提供统一的消息发送抽象
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface PulsarMessageSender {

    /**
     * 同步发送消息到默认主题
     *
     * @param message 消息内容
     * @return 消息ID
     */
    MessageId send(Object message);

    /**
     * 同步发送消息到指定主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 消息ID
     */
    MessageId send(String topic, Object message);

    /**
     * 同步发送消息到指定主题（带消息键）
     *
     * @param topic   主题名称
     * @param key     消息键
     * @param message 消息内容
     * @return 消息ID
     */
    MessageId send(String topic, String key, Object message);

    /**
     * 异步发送消息到默认主题
     *
     * @param message 消息内容
     * @return 异步消息ID
     */
    CompletableFuture<MessageId> sendAsync(Object message);

    /**
     * 异步发送消息到指定主题
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 异步消息ID
     */
    CompletableFuture<MessageId> sendAsync(String topic, Object message);

    /**
     * 异步发送消息到指定主题（带消息键）
     *
     * @param topic   主题名称
     * @param key     消息键
     * @param message 消息内容
     * @return 异步消息ID
     */
    CompletableFuture<MessageId> sendAsync(String topic, String key, Object message);

    /**
     * 发送延迟消息
     *
     * @param topic       主题名称
     * @param message     消息内容
     * @param delayMillis 延迟毫秒数
     * @return 消息ID
     */
    MessageId sendDelayed(String topic, Object message, long delayMillis);

    /**
     * 发送事务消息
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 消息ID
     */
    MessageId sendInTransaction(String topic, Object message);
}