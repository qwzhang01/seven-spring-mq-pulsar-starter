package com.github.spring.mq.pulsar.exception;

/**
 * Base Pulsar exception
 *
 * <p>This is the base exception class for all Pulsar-related exceptions
 * in the Seven Spring MQ Pulsar Starter. All specific Pulsar exceptions
 * should extend this class.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarException extends RuntimeException {
    public PulsarException(String msg) {
        super(msg);
    }

    public PulsarException(String msg, Throwable cause) {
        super(msg, cause);
    }
}