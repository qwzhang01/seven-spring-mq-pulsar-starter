package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar 死信队列配置类
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarDeadLetterConfiguration {
    /**
     * 死信重试策略
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterMessageProcessor deadLetterMessageProcessor() {
        return new DeadLetterMessageProcessor();
    }
}