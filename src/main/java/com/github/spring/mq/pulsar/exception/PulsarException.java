package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 异常
 *
 * @author avinzhang
 */
public class PulsarException extends RuntimeException {
    public PulsarException(String msg) {
        super(msg);
    }

    public PulsarException(String msg, Throwable cause) {
        super(msg, cause);
    }
}