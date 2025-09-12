package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.LoggingPulsarMessageInterceptor;
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
public class PulsarLogInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoggingPulsarMessageInterceptor loggingPulsarMessageInterceptor() {
        return new LoggingPulsarMessageInterceptor();
    }
}