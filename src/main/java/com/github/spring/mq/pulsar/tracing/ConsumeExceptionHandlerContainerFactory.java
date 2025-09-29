package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;

import java.lang.reflect.Method;

/**
 * Factory for creating message consumption exception handler containers
 * 
 * <p>This factory is responsible for creating and managing exception handler
 * containers that process exceptions thrown during message consumption.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class ConsumeExceptionHandlerContainerFactory {
    private final ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer;

    public ConsumeExceptionHandlerContainerFactory(ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        this.consumeExceptionHandlerContainer = consumeExceptionHandlerContainer;
    }

    public void create(Object bean, Method method, ConsumerExceptionHandler annotation) {
        consumeExceptionHandlerContainer.create(bean, method, annotation);
    }
}
