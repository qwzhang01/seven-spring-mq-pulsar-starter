package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.health.PulsarHealthIndicator;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar health check configuration class
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarHealthConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public PulsarHealthIndicator pulsarHealthIndicator(PulsarClient pulsarClient) {
        return new PulsarHealthIndicator(pulsarClient);
    }
}