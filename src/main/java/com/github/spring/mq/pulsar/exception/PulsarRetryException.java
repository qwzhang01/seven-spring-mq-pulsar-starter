package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 消息重试异常
 *
 * @author avinzhang
 */
public class PulsarRetryException extends PulsarException {

    public PulsarRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}