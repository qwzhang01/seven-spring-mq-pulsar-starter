package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

/**
 * Pulsar interceptor configuration class
 * 
 * <p>This configuration class sets up the interceptor infrastructure for Pulsar
 * message processing. It automatically discovers all PulsarMessageInterceptor
 * beans and registers them in a sorted registry based on their priority.
 * 
 * <p>Features:
 * <ul>
 *   <li>Automatic interceptor discovery and registration</li>
 *   <li>Priority-based interceptor ordering</li>
 *   <li>Conditional configuration based on PulsarClient presence</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PulsarInterceptorRegistry pulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        return new PulsarInterceptorRegistry(interceptors);
    }

    /**
     * Interceptor registry
     * 
     * <p>A record that holds all registered PulsarMessageInterceptor instances
     * sorted by their priority order. Lower order values indicate higher priority.
     * 
     * @param interceptors List of interceptors sorted by priority
     */
    public record PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        public PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
            this.interceptors = interceptors;
            // Sort by priority order
            this.interceptors.sort(Comparator.comparingInt(PulsarMessageInterceptor::getOrder));
        }
    }
}