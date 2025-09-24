package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar message retry exception
 *
 * <p>This exception is thrown when there are errors during message retry
 * operations, such as retry limit exceeded, retry queue failures,
 * or retry policy violations.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarRetryException extends PulsarException {

    public PulsarRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}