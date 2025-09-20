package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar configuration unsupported exception
 * 
 * <p>This exception is thrown when an unsupported or invalid configuration
 * is detected, such as conflicting settings, deprecated options,
 * or mutually exclusive configurations.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConfigUnsupportedException extends PulsarException {
    public PulsarConfigUnsupportedException(String msg) {
        super(msg);
    }
}