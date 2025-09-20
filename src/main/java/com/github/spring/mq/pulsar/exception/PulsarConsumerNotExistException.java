package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar consumer not exist exception
 * 
 * <p>This exception is thrown when attempting to access or use a Pulsar consumer
 * that does not exist or has not been properly initialized. This can occur when
 * referencing a consumer by name that was never created or has been destroyed.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConsumerNotExistException extends PulsarException {
    public PulsarConsumerNotExistException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarConsumerNotExistException(String msg) {
        super(msg);
    }
}