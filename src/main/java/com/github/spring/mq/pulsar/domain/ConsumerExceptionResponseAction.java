package com.github.spring.mq.pulsar.domain;

/**
 * 消息消费异常后，响应方式
 * <p>
 * ACK: 确认消费
 * NACK: 拒绝消费
 * RECONSUME_LATER: 延迟消费
 *
 * @author avinzhang
 */
public enum ConsumerExceptionResponseAction {
    ACK, NACK, RECONSUME_LATER
}