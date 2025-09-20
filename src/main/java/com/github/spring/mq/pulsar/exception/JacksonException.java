package com.github.spring.mq.pulsar.exception;

/**
 * Jackson serialization/deserialization exception
 * 
 * <p>This exception is thrown when there are errors during JSON serialization
 * or deserialization using Jackson library, such as invalid JSON format,
 * missing fields, or type conversion issues.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class JacksonException extends PulsarException {
    public JacksonException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
