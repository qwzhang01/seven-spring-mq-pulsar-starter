package com.github.spring.mq.pulsar.annotation;

import java.lang.annotation.*;

/**
 * Annotation for marking methods as consumer exception handlers
 * 
 * <p>This annotation is used to mark methods that should handle exceptions
 * thrown during message consumption. Methods annotated with this annotation
 * will be automatically registered as exception handlers and invoked when
 * matching exceptions occur during message processing.
 * 
 * <p>Example usage:
 * <pre>
 * &#64;ConsumerExceptionHandler({IllegalArgumentException.class, ValidationException.class})
 * &#64;ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
 * public void handleValidationErrors(Exception ex) {
 *     // Handle validation errors
 * }
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
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