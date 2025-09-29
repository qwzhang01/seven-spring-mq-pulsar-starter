package com.github.spring.mq.pulsar.annotation;

import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;

import java.lang.annotation.*;

/**
 * 消息消费异常处理器注释
 *
 * @author avinzhang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsumerExceptionResponse {

    /**
     * Exceptions handled by the annotated method. If empty, will default to any
     * exceptions listed in the method argument list.
     */
    ConsumerExceptionResponseAction value() default ConsumerExceptionResponseAction.NACK;
}
