package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar consumer later exception
 * 
 * <p>This exception is thrown when a message needs to be reprocessed later,
 * typically used for retry scenarios where the message processing should be
 * delayed and attempted again.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConsumerLatterException extends PulsarException {
    public PulsarConsumerLatterException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public PulsarConsumerLatterException(String msg) {
        super(msg);
    }
}