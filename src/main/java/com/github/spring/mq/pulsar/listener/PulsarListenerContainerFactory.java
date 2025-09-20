package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import org.apache.pulsar.client.api.Consumer;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulsar listener container factory
 * 
 * <p>This factory class is responsible for creating and managing PulsarListenerContainer instances.
 * It handles the creation of containers for methods annotated with @PulsarListener and manages
 * the associated Pulsar consumers.
 * 
 * <p>Features:
 * <ul>
 *   <li>Container creation and caching</li>
 *   <li>Consumer configuration management</li>
 *   <li>Multiple listeners per topic support</li>
 *   <li>Automatic consumer lifecycle management</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarListenerContainerFactory {

    private final PulsarProperties pulsarProperties;
    private final PulsarTemplate pulsarTemplate;
    private final ListenerType listenerType;
    private final ConcurrentHashMap<String, PulsarListenerContainer> containerCache = new ConcurrentHashMap<>();

    public PulsarListenerContainerFactory(PulsarProperties pulsarProperties,
                                          PulsarTemplate pulsarTemplate, ListenerType listenerType) {
        this.pulsarProperties = pulsarProperties;
        this.pulsarTemplate = pulsarTemplate;
        this.listenerType = listenerType;
    }

    /**
     * Create listener container
     * 
     * <p>Creates a new PulsarListenerContainer for the given bean method and annotation.
     * If a container already exists for the topic, the method will be added to the existing container.
     * 
     * @param bean Bean instance containing the listener method
     * @param method Listener method annotated with @PulsarListener
     * @param annotation PulsarListener annotation containing configuration
     * @return PulsarListenerContainer instance
     * @throws IllegalArgumentException if consumer property or topic is null
     */
    public PulsarListenerContainer createContainer(Object bean, Method method,
                                                   PulsarListener annotation) {
        if (containerCache.containsKey(annotation.topic())) {
            PulsarListenerContainer container = containerCache.get(annotation.topic());
            container.addMethod(bean, method, annotation);
            return container;
        }

        PulsarProperties.Consumer consumerProperty = null;
        Map<String, PulsarProperties.Consumer> consumerMap = pulsarProperties.getConsumerMap();
        if (consumerMap == null || consumerMap.isEmpty()) {
            consumerProperty = pulsarProperties.getConsumer();
        } else {
            consumerProperty = consumerMap.get(annotation.topic());
        }
        if (consumerProperty == null) {
            consumerProperty = pulsarProperties.getConsumer();
        }
        if (consumerProperty == null) {
            throw new IllegalArgumentException("consumer property is null");
        }

        if (!StringUtils.hasText(consumerProperty.getTopic())) {
            throw new IllegalArgumentException("consumer topic is null");
        }

        Consumer<byte[]> consumer = pulsarTemplate.getOrCreateConsumer(annotation.consumerName(),
                consumerProperty, listenerType, containerCache);

        PulsarListenerContainer container = new PulsarListenerContainer(consumer,
                bean,
                annotation.msgRoute(),
                method,
                annotation.routeKey(),
                annotation.dataKey(),
                consumerProperty.isAutoAck(),
                annotation.messageType(),
                pulsarTemplate,
                listenerType);

        containerCache.put(annotation.topic(), container);
        return container;
    }
}