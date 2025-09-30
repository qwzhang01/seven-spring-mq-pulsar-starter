/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
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
    private final ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer;

    public PulsarListenerContainerFactory(PulsarProperties pulsarProperties,
                                          PulsarTemplate pulsarTemplate,
                                          ListenerType listenerType,
                                          ConsumeExceptionHandlerContainer consumeExceptionHandlerContainer) {
        this.pulsarProperties = pulsarProperties;
        this.pulsarTemplate = pulsarTemplate;
        this.listenerType = listenerType;
        this.consumeExceptionHandlerContainer = consumeExceptionHandlerContainer;
    }

    /**
     * Create listener container
     *
     * <p>Creates a new PulsarListenerContainer for the given bean method and annotation.
     * If a container already exists for the topic, the method will be added to the existing container.
     *
     * @param bean       Bean instance containing the listener method
     * @param method     Listener method annotated with @PulsarListener
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
                listenerType, consumeExceptionHandlerContainer);

        containerCache.put(annotation.topic(), container);
        return container;
    }
}