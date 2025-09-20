package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar consumer initialization exception
 * 
 * <p>This exception is thrown when there are errors during Pulsar consumer
 * initialization, such as subscription failures, topic access issues,
 * or consumer configuration problems.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConsumeInitException extends PulsarException {
    public PulsarConsumeInitException(String msg, Throwable cause) {
        super(msg, cause);
    }
}