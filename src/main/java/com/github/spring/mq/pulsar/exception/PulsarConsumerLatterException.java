package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 生产者初始化异常
 *
 * @author avinzhang
 */
public class PulsarConsumerLatterException extends PulsarException {
    public PulsarConsumerLatterException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarConsumerLatterException(String msg) {
        super(msg);
    }
}