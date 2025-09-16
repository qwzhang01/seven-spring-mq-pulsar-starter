package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;
import org.springframework.util.StringUtils;

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
    public PulsarListenerContainer createContainer(Object bean, Method method, PulsarListener annotation) {
        if (containerCache.containsKey(annotation.topic())) {
            PulsarListenerContainer container = containerCache.get(annotation.topic());
            container.addMethod(annotation.businessPath(), method, annotation.businessKey(), annotation.dataKey(), annotation.messageType());
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
            throw new IllegalArgumentException("consumer property is null");
        }

        if (!StringUtils.hasText(consumerProperty.getTopic())) {
            throw new IllegalArgumentException("consumer topic is null");
        }

        Consumer<byte[]> consumer = pulsarTemplate.getOrCreateConsumer(annotation.consumerName(), consumerProperty);
        PulsarListenerContainer container = new PulsarListenerContainer(consumer, bean,
                annotation.businessPath(), method, annotation.businessKey(), annotation.dataKey(),
                consumerProperty.isAutoAck(), annotation.messageType(), pulsarTemplate);
        containerCache.put(annotation.topic(), container);
        return container;
    }
}