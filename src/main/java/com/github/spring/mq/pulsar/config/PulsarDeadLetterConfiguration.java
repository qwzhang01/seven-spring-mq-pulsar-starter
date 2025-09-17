package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.deadletter.DeadLetterMessageProcessor;
import com.github.spring.mq.pulsar.deadletter.DeadLetterQueueManager;
import com.github.spring.mq.pulsar.deadletter.DeadLetterRetryStrategy;
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

    /**
     * 死信队列处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultDeadLetterQueueHandler defaultDeadLetterQueueHandler(PulsarTemplate pulsarTemplate,
                                                                       DeadLetterMessageProcessor deadLetterMessageProcessor) {
        return new DefaultDeadLetterQueueHandler(pulsarTemplate, deadLetterMessageProcessor);
    }

    /**
     * 死信消息处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterMessageProcessor deadLetterMessageProcessor(PulsarTemplate pulsarTemplate) {
        return new DeadLetterMessageProcessor(pulsarTemplate);
    }

    /**
     * 死信队列管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterQueueManager deadLetterQueueManager(PulsarTemplate pulsarTemplate, DeadLetterMessageProcessor deadLetterMessageProcessor) {
        return new DeadLetterQueueManager(pulsarTemplate, deadLetterMessageProcessor);
    }

    /**
     * 死信重试策略
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterRetryStrategy deadLetterRetryStrategy() {
        return new DeadLetterRetryStrategy();
    }
}