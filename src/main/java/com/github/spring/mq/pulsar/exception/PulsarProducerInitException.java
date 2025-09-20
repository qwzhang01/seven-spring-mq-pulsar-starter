package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar producer initialization exception
 * 
 * <p>This exception is thrown when there are errors during Pulsar producer
 * initialization, such as connection failures, authentication issues,
 * or resource allocation problems.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarProducerInitException extends PulsarException {
    public PulsarProducerInitException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarProducerInitException(String msg) {
        super(msg);
    }
}