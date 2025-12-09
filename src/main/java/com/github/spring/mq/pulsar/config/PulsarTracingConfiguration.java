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

package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.TracingPulsarMessageInterceptor;
import com.github.spring.mq.pulsar.tracing.ConsumeDefaultExceptionHandler;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainerFactory;
import com.github.spring.mq.pulsar.tracing.ConsumerAdviceAnnotationBeanPostProcessor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar tracing auto-configuration
 *
 * <p>This configuration class automatically sets up distributed tracing for Pulsar
 * when Micrometer Tracing is available on the classpath. It:
 * <ul>
 *   <li>Creates a tracing interceptor when a Tracer bean is available</li>
 *   <li>Configures trace propagation through message headers</li>
 *   <li>Can be disabled via configuration properties</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.2.16
 */
@Configuration
@ConditionalOnClass(Tracer.class)
@ConditionalOnProperty(name = "pulsar.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class PulsarTracingConfiguration {

    /**
     * Creates a tracing interceptor for Pulsar messages
     *
     * @param tracer the Micrometer tracer
     * @return the tracing interceptor
     */
    @Bean
    @ConditionalOnMissingBean
    public TracingPulsarMessageInterceptor tracingPulsarMessageInterceptor(Tracer tracer, Propagator propagato) {
        return new TracingPulsarMessageInterceptor(tracer,propagato);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumeDefaultExceptionHandler consumeDefaultExceptionHandler() {
        return new ConsumeDefaultExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerAdviceAnnotationBeanPostProcessor consumerAdviceAnnotationBeanPostProcessor(ConsumeExceptionHandlerContainerFactory containerFactory) {
        return new ConsumerAdviceAnnotationBeanPostProcessor(containerFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumeExceptionHandlerContainerFactory consumeExceptionHandlerContainerFactory(ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        return new ConsumeExceptionHandlerContainerFactory(consumeExceptionHandlerContainer);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer() {
        return new ConsumeExceptionHandlerContainer();
    }
}