package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainerFactory;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Pulsar consumer loop configuration
 *
 * <p>This configuration class provides automatic setup for loop-based
 * message consumption when PulsarClient is available in the application context.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(PulsarClient.class)
public class PulsarConsumerLoopConfiguration {

    /**
     * Create listener container factory for loop-based consumption
     *
     * @param pulsarProperties Pulsar configuration properties
     * @param pulsarTemplate   Pulsar template for message operations
     * @return PulsarListenerContainerFactory configured for loop mode
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate, ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        return new PulsarListenerContainerFactory(pulsarProperties, pulsarTemplate, ListenerType.LOOP, consumeExceptionHandlerContainer);
    }

}