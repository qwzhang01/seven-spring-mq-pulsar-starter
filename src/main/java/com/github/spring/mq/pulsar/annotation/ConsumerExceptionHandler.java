package com.github.spring.mq.pulsar.annotation;

import java.lang.annotation.*;

/**
 * 消息消费异常处理器注释
 *
 * @author avinzhang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsumerExceptionHandler {

    /**
     * Exceptions handled by the annotated method. If empty, will default to any
     * exceptions listed in the method argument list.
     */
    Class<? extends Throwable>[] value() default {};
}