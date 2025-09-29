package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.PerformancePulsarMessageInterceptor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar performance interceptor configuration
 *
 * <p>This configuration class provides automatic setup for performance monitoring
 * interceptor when PulsarClient is available in the application context.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarPerformanceInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PerformancePulsarMessageInterceptor performancePulsarMessageInterceptor() {
        return new PerformancePulsarMessageInterceptor();
    }
}