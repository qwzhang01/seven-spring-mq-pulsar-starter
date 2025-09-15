package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 生产者初始化异常
 *
 * @author avinzhang
 */
public class PulsarConsumerNotExistException extends PulsarException {
    public PulsarConsumerNotExistException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarConsumerNotExistException(String msg) {
        super(msg);
    }
}