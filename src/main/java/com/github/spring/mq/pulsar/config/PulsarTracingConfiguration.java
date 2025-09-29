package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.TracingPulsarMessageInterceptor;
import com.github.spring.mq.pulsar.tracing.ConsumeDefaultExceptionHandler;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainerFactory;
import com.github.spring.mq.pulsar.tracing.ConsumerAdviceAnnotationBeanPostProcessor;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
    @ConditionalOnBean(Tracer.class)
    public TracingPulsarMessageInterceptor tracingPulsarMessageInterceptor(Tracer tracer) {
        return new TracingPulsarMessageInterceptor(tracer);
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