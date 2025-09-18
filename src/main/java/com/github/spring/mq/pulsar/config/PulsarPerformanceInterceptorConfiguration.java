package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.PerformancePulsarMessageInterceptor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar 拦截器配置类
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