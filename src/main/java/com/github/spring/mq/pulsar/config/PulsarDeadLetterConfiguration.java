package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.deadletter.DeadLetterQueueHandler;
import com.github.spring.mq.pulsar.deadletter.DefaultDeadLetterQueueHandler;
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
public class PulsarDeadLetterConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DeadLetterQueueHandler deadLetterQueueHandler() {
        return new DefaultDeadLetterQueueHandler();
    }
}