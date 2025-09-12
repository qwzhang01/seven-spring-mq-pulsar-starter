package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

/**
 * Pulsar 拦截器配置类
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
public class PulsarInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PulsarInterceptorRegistry pulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        return new PulsarInterceptorRegistry(interceptors);
    }

    /**
     * 拦截器注册表
     */
    public record PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        public PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
            this.interceptors = interceptors;
            // 按优先级排序
            this.interceptors.sort(Comparator.comparingInt(PulsarMessageInterceptor::getOrder));
        }
    }
}