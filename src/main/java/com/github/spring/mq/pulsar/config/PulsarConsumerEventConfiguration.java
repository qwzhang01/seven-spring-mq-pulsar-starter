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
 * Pulsar consumer event configuration class
 *
 * <p>This auto-configuration class sets up event-driven Pulsar consumers.
 * It creates the necessary infrastructure for handling Pulsar messages
 * using event-driven listeners rather than polling-based consumption.
 *
 * <p>The configuration is conditional on the presence of a PulsarClient bean
 * and creates a PulsarListenerContainerFactory configured for EVENT listener type.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnBean(PulsarClient.class)
public class PulsarConsumerEventConfiguration {

    /**
     * Create listener container factory for event-driven consumers
     *
     * <p>Creates a PulsarListenerContainerFactory configured with EVENT listener type
     * for handling message consumption through event callbacks.
     *
     * @param pulsarProperties Pulsar configuration properties
     * @param pulsarTemplate   Pulsar template for message operations
     * @return PulsarListenerContainerFactory configured for event-driven consumption
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate, ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        return new PulsarListenerContainerFactory(pulsarProperties, pulsarTemplate, ListenerType.EVENT, consumeExceptionHandlerContainer);
    }
}