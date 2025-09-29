package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;

import java.lang.reflect.Method;

/**
 * 消息消费异常处理器容器工厂
 *
 * @author avinzhang
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
