package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 发送消息异常
 *
 * @author avinzhang
 */
public class PulsarConsumeReceiveException extends PulsarException {
    public PulsarConsumeReceiveException(Throwable cause) {
        super("消息消费异常", cause);
    }

    public PulsarConsumeReceiveException(String message, Throwable cause) {
        super(message, cause);
    }
}