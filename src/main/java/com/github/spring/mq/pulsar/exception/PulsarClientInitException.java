package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar client 启动异常
 *
 * @author avinzhang
 */
public class PulsarClientInitException extends PulsarException {
    public PulsarClientInitException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
