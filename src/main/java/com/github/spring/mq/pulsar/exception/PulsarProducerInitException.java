package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 生产者初始化异常
 *
 * @author avinzhang
 */
public class PulsarProducerInitException extends PulsarException {
    public PulsarProducerInitException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarProducerInitException(String msg) {
        super(msg);
    }
}