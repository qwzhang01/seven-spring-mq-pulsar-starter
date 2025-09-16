package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 发送消息异常
 *
 * @author avinzhang
 */
public class PulsarProducerSendException extends PulsarException {
    public PulsarProducerSendException(Throwable cause) {
        super("消息发送异常", cause);
    }

    public PulsarProducerSendException(String message, Throwable cause) {
        super(message, cause);
    }
}