package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 消费者初始化异常
 *
 * @author avinzhang
 */
public class PulsarConsumeInitException extends PulsarException {
    public PulsarConsumeInitException(String msg, Throwable cause) {
        super(msg, cause);
    }
}