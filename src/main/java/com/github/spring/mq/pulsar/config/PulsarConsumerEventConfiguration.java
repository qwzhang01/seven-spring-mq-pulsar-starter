package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.core.DefaultPulsarMessageReceiver;
import com.github.spring.mq.pulsar.core.PulsarMessageReceiver;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.domain.ListenerType;
import com.github.spring.mq.pulsar.listener.PulsarListenerAnnotationBeanPostProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Pulsar 消费者自动配置类
 *
 * @author avinzhang
 */
@AutoConfiguration
public class PulsarConsumerEventConfiguration {

    /**
     * 创建消息接收器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarMessageReceiver pulsarMessageReceiver(PulsarTemplate pulsarTemplate) {
        return new DefaultPulsarMessageReceiver(pulsarTemplate);
    }

    /**
     * 创建监听器容器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarProperties pulsarProperties, PulsarTemplate pulsarTemplate) {
        return new PulsarListenerContainerFactory(pulsarProperties, pulsarTemplate, ListenerType.EVENT);
    }

    /**
     * 创建监听器注解处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerAnnotationBeanPostProcessor pulsarListenerAnnotationBeanPostProcessor(PulsarListenerContainerFactory containerFactory) {
        return new PulsarListenerAnnotationBeanPostProcessor(containerFactory);
    }
}