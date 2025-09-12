package com.github.spring.mq.pulsar.exception;

/**
 * Jackson 序列化 反序列化异常
 *
 * @author avinzhang
 */
public class JacksonException extends PulsarException {
    public JacksonException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
