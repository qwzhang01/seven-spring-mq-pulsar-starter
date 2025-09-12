package com.github.spring.mq.pulsar.deadletter;

import org.apache.pulsar.client.api.Message;

/**
 * 死信队列处理器接口
 * 用于处理消费失败的消息
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface DeadLetterQueueHandler {

    /**
     * 处理死信消息
     *
     * @param originalTopic 原始主题
     * @param message       死信消息
     * @param exception     处理异常
     */
    void handleDeadLetter(String originalTopic, Message<?> message, Exception exception);

    /**
     * 获取死信队列主题名称
     *
     * @param originalTopic 原始主题
     * @return 死信队列主题名称
     */
    default String getDeadLetterTopic(String originalTopic) {
        return originalTopic + "-dlq";
    }

    /**
     * 判断是否应该发送到死信队列
     *
     * @param message    消息
     * @param exception  异常
     * @param retryCount 重试次数
     * @return 是否发送到死信队列
     */
    default boolean shouldSendToDeadLetter(Message<?> message, Exception exception, int retryCount) {
        return retryCount >= getMaxRetries();
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    default int getMaxRetries() {
        return 3;
    }
}