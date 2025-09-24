package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar dead letter queue configuration class
 *
 * <p>This configuration class sets up the infrastructure for handling
 * dead letter messages in Pulsar. Dead letter messages are messages
 * that have exceeded the maximum retry attempts and need special handling.
 *
 * <p>The configuration provides a DeadLetterMessageProcessor bean that
 * handles the processing and acknowledgment of dead letter messages.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarDeadLetterConfiguration {
    /**
     * Dead letter message processor
     *
     * <p>Creates a DeadLetterMessageProcessor bean for handling messages
     * that have been moved to the dead letter queue after exceeding
     * the maximum retry attempts.
     *
     * @return DeadLetterMessageProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterMessageProcessor deadLetterMessageProcessor() {
        return new DeadLetterMessageProcessor();
    }
}