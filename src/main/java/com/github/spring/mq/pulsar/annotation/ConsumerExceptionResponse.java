package com.github.spring.mq.pulsar.annotation;

import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;

import java.lang.annotation.*;

/**
 * Annotation for defining consumer exception response actions
 * 
 * <p>This annotation is used in conjunction with {@link ConsumerExceptionHandler}
 * to specify how exceptions should be handled during message consumption.
 * It defines the response action that should be taken when an exception occurs.
 *
 * @author avinzhang
 * @since 1.0.0
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
