package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar client initialization exception
 * 
 * <p>This exception is thrown when there are errors during Pulsar client
 * initialization, such as connection failures, authentication issues,
 * or configuration problems.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarClientInitException extends PulsarException {
    public PulsarClientInitException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
