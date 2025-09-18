package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainerFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Pulsar 消费者自动配置类
 *
 * @author avinzhang
 */
@AutoConfiguration
@ConditionalOnBean(PulsarClient.class)
public class PulsarConsumerEventConfiguration {

    /**
     * 创建监听器容器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate) {
        return new PulsarListenerContainerFactory(pulsarProperties, pulsarTemplate, ListenerType.EVENT);
    }
}