package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar 生产者配置异常
 *
 * @author avinzhang
 */
public class PulsarProducerConfigException extends PulsarException {

    public PulsarProducerConfigException(String msg) {
        super(msg);
    }
}