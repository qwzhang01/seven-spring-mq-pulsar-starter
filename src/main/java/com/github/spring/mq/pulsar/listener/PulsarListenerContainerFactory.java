package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulsar 监听器容器工厂
 *
 * @author avinzhang
 */
public class PulsarListenerContainerFactory {

    private final PulsarProperties pulsarProperties;
    private final PulsarTemplate pulsarTemplate;

    private final ConcurrentHashMap<String, PulsarListenerContainer> containerCache = new ConcurrentHashMap<>();

    public PulsarListenerContainerFactory(PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate) {
        this.pulsarProperties = pulsarProperties;
        this.pulsarTemplate = pulsarTemplate;
    }

    /**
     * 创建监听器容器
     */
    public PulsarListenerContainer createContainer(Object bean, String businessPath, Method method, PulsarListener annotation) {
        if (containerCache.containsKey(annotation.topic())) {
            PulsarListenerContainer container = containerCache.get(annotation.topic());
            container.addMethod(businessPath, method, annotation.businessKey());
            return container;
        }

        PulsarProperties.Consumer consumerProperty = null;
        Map<String, PulsarProperties.Consumer> consumerMap = pulsarProperties.getConsumerMap();
        if (consumerMap == null || consumerMap.isEmpty()) {
            consumerProperty = pulsarProperties.getConsumer();
        } else {
            consumerProperty = consumerMap.get(pulsarProperties.getConsumer().getTopic());
        }
        Consumer<byte[]> consumer = pulsarTemplate.getOrCreateConsumer(annotation.consumerName(), annotation.subscription(), annotation.subscriptionType(), consumerProperty);
        return new PulsarListenerContainer(consumer, bean, businessPath, method, annotation.businessKey(), annotation.autoAck(), annotation.messageType(), pulsarTemplate);
    }
}