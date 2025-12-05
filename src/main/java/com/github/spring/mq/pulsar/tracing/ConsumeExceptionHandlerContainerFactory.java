/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
