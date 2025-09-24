package com.github.spring.mq.pulsar.exception;

/**
 * Pulsar producer configuration exception
 *
 * <p>This exception is thrown when there are configuration errors
 * related to Pulsar producers, such as invalid topic names,
 * missing required properties, or conflicting settings.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarProducerConfigException extends PulsarException {

    public PulsarProducerConfigException(String msg) {
        super(msg);
    }
}