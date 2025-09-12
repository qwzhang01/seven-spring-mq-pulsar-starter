package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.exception.PulsarConsumeInitException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SubscriptionType;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 监听器容器工厂
 *
 * @author avinzhang
 */
public class PulsarListenerContainerFactory {

    private final PulsarClient pulsarClient;
    private final PulsarProperties pulsarProperties;
    private final PulsarTemplate pulsarTemplate;
    private final ConcurrentHashMap<String, PulsarListenerContainer> consumerCache = new ConcurrentHashMap<>();

    public PulsarListenerContainerFactory(PulsarClient pulsarClient, PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate) {
        this.pulsarClient = pulsarClient;
        this.pulsarProperties = pulsarProperties;
        this.pulsarTemplate = pulsarTemplate;
    }

    /**
     * 创建监听器容器
     */
    public PulsarListenerContainer createContainer(Object bean, Method method, PulsarListener annotation) {
        return consumerCache.computeIfAbsent(annotation.topic(), t -> {
            String consumerName = StringUtils.hasText(annotation.consumerName()) ? annotation.consumerName() :
                    UUID.randomUUID().toString().replace("-", "").toLowerCase();

            PulsarProperties.Consumer consumerProperty = null;
            Map<String, PulsarProperties.Consumer> consumerMap = pulsarProperties.getConsumerMap();
            if (consumerMap == null || consumerMap.isEmpty()) {
                consumerProperty = pulsarProperties.getConsumer();
            } else {
                consumerProperty = consumerMap.get(annotation.topic());
            }

            try {
                Consumer<byte[]> consumer = pulsarClient.newConsumer()
                        .topic("persistent://" + consumerProperty.getTopic())
                        .subscriptionName(StringUtils.hasText(annotation.subscription())
                                ? annotation.subscription()
                                : pulsarProperties.getConsumer().getSubscriptionName())
                        .subscriptionType(SubscriptionType.valueOf(annotation.subscriptionType()))
                        .consumerName(consumerName)
                        .ackTimeout(pulsarProperties.getConsumer().getAckTimeout().toMillis(), TimeUnit.MILLISECONDS)
                        .receiverQueueSize(pulsarProperties.getConsumer().getReceiverQueueSize())
                        .autoAckOldestChunkedMessageOnQueueFull(pulsarProperties.getConsumer().isAutoAckOldestChunkedMessageOnQueueFull())
                        .subscribe();

                return new PulsarListenerContainer(consumer, bean, method, annotation.autoAck(), annotation.messageType(), this.pulsarTemplate);
            } catch (PulsarClientException e) {
                throw new PulsarConsumeInitException("Failed to create consumer for topic: " + t, e);
            }
        });
    }
}