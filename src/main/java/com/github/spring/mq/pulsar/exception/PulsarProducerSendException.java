package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar producer send exception
 *
 * <p>This exception is thrown when there are errors during message sending
 * to Pulsar topics, such as network issues, topic not found, or producer
 * configuration problems.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarProducerSendException extends PulsarException {
    public PulsarProducerSendException(Throwable cause) {
        super("Message send exception", cause);
    }

    public PulsarProducerSendException(String message, Throwable cause) {
        super(message, cause);
    }
}