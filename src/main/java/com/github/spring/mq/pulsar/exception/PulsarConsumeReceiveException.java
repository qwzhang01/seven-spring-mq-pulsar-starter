package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar consume receive exception
 *
 * <p>This exception is thrown when there are errors during message consumption
 * from Pulsar topics, such as deserialization failures, consumer connection
 * issues, or message acknowledgment problems.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConsumeReceiveException extends PulsarException {
    public PulsarConsumeReceiveException(Throwable cause) {
        super("Message consumption exception", cause);
    }

    public PulsarConsumeReceiveException(String message, Throwable cause) {
        super(message, cause);
    }
}