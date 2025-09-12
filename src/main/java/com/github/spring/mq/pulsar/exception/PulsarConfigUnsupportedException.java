package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 配置不支持异常
 *
 * @author avinzhang
 */
public class PulsarConfigUnsupportedException extends PulsarException {
    public PulsarConfigUnsupportedException(String msg) {
        super(msg);
    }
}