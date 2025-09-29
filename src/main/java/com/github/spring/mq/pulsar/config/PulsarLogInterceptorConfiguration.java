package com.github.spring.mq.pulsar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.interceptor.LoggingPulsarMessageInterceptor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar logging interceptor configuration
 *
 * <p>This configuration class provides automatic setup for message logging
 * interceptor when PulsarClient is available in the application context.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarLogInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoggingPulsarMessageInterceptor loggingPulsarMessageInterceptor(ObjectMapper objectMapper) {
        return new LoggingPulsarMessageInterceptor(objectMapper);
    }
}