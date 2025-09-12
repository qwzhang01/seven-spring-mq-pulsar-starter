package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;

/**
 * Pulsar 消息拦截器接口
 * 可以在消息发送和接收过程中进行拦截处理
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface PulsarMessageInterceptor {

    /**
     * 发送消息前拦截
     *
     * @param topic   主题名称
     * @param message 消息内容
     * @return 处理后的消息内容，返回 null 则不发送消息
     */
    default Object beforeSend(String topic, Object message) {
        return message;
    }

    /**
     * 发送消息后拦截
     *
     * @param topic     主题名称
     * @param message   消息内容
     * @param messageId 消息ID
     * @param exception 发送异常（如果有）
     */
    default void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        // 默认空实现
    }

    /**
     * 接收消息前拦截
     *
     * @param message 原始消息
     * @return 是否继续处理消息
     */
    default boolean beforeReceive(Message<?> message) {
        return true;
    }

    /**
     * 接收消息后拦截
     *
     * @param message          原始消息
     * @param processedMessage 处理后的消息内容
     * @param exception        处理异常（如果有）
     */
    default void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // 默认空实现
    }

    /**
     * 获取拦截器优先级
     * 数值越小优先级越高
     *
     * @return 优先级
     */
    default int getOrder() {
        return 0;
    }
}