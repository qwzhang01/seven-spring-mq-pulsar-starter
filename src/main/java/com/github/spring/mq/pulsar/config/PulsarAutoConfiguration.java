package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.core.*;
import com.github.spring.mq.pulsar.exception.PulsarClientInitException;
import com.github.spring.mq.pulsar.listener.PulsarListenerAnnotationBeanPostProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 自动配置类
 *
 * @author avinzhang
 */
@AutoConfiguration
@ConditionalOnClass(PulsarClient.class)
@EnableConfigurationProperties(PulsarProperties.class)
public class PulsarAutoConfiguration {

    private final PulsarProperties pulsarProperties;
    Log log = LogFactory.getLog(PulsarAutoConfiguration.class);

    public PulsarAutoConfiguration(PulsarProperties pulsarProperties) {
        this.pulsarProperties = pulsarProperties;
    }

    /**
     * 创建 Pulsar 客户端
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarClient pulsarClient() throws PulsarClientException {
        // 校验配置参数
        pulsarProperties.valid();
        // 初始化 Pulsar 客户端
        try {
            ClientBuilder clientBuilder = PulsarClient.builder()
                    .serviceUrl(pulsarProperties.getServiceUrl())
                    .operationTimeout((int) pulsarProperties.getClient().getOperationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .connectionTimeout((int) pulsarProperties.getClient().getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    // netty的ioThreads负责网络IO操作，如果业务流量较大，可以调高ioThreads个数
                    .ioThreads(pulsarProperties.getClient().getNumIoThreads())
                    // 负责调用以listener模式启动的消费者的回调函数，建议配置大于该client负责的partition数目；
                    .listenerThreads(pulsarProperties.getClient().getNumListenerThreads());

            // 配置认证
            PulsarProperties.Authentication auth = pulsarProperties.getAuthentication();
            if (auth.isEnabled()) {
                if (StringUtils.hasText(auth.getToken())) {
                    clientBuilder.authentication("org.apache.pulsar.client.impl.auth.AuthenticationToken", auth.getToken());
                } else if (StringUtils.hasText(auth.getAuthPluginClassName())) {
                    clientBuilder.authentication(auth.getAuthPluginClassName(), auth.getAuthParams());
                }
            }

            PulsarClient client = clientBuilder.build();
            log.info("Pulsar client created success.");
            return client;
        } catch (Exception e) {
            throw new PulsarClientInitException("Failed to create Pulsar client", e);
        }
    }

    /**
     * 创建 Pulsar 模板
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarTemplate pulsarTemplate(PulsarClient pulsarClient,
                                         PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry) {
        PulsarTemplate template = new PulsarTemplate(pulsarClient, pulsarProperties);
        template.setInterceptorRegistry(interceptorRegistry);
        return template;
    }

    /**
     * 创建监听器容器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarClient pulsarClient, PulsarTemplate pulsarTemplate) {
        return new PulsarListenerContainerFactory(pulsarClient, pulsarProperties, pulsarTemplate);
    }

    /**
     * 创建监听器注解处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerAnnotationBeanPostProcessor pulsarListenerAnnotationBeanPostProcessor(
            PulsarListenerContainerFactory containerFactory) {
        return new PulsarListenerAnnotationBeanPostProcessor(containerFactory);
    }

    /**
     * 创建消息发送器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarMessageSender pulsarMessageSender(PulsarTemplate pulsarTemplate) {
        return new DefaultPulsarMessageSender(pulsarTemplate, pulsarProperties);
    }

    /**
     * 创建多生产者 Bean 注册器
     *
     * @param pulsarTemplate
     * @param applicationContext
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public MultipleProducerBeanRegistrar multipleProducerBeanRegistrar(PulsarTemplate pulsarTemplate, ApplicationContext applicationContext) {
        MultipleProducerBeanRegistrar multipleProducerBeanRegistrar = new MultipleProducerBeanRegistrar();
        multipleProducerBeanRegistrar.setPulsarProperties(pulsarProperties);
        multipleProducerBeanRegistrar.setPulsarTemplate(pulsarTemplate);
        return multipleProducerBeanRegistrar;
    }

    /**
     * 创建消息接收器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarMessageReceiver pulsarMessageReceiver(PulsarTemplate pulsarTemplate) {
        return new DefaultPulsarMessageReceiver(pulsarTemplate);
    }

    /**
     * 多生产者 Bean 注册器
     */
    public static class MultipleProducerBeanRegistrar implements ApplicationContextAware, InitializingBean {

        private ApplicationContext applicationContext;
        private PulsarProperties pulsarProperties;
        private PulsarTemplate pulsarTemplate;

        public void setPulsarTemplate(PulsarTemplate pulsarTemplate) {
            this.pulsarTemplate = pulsarTemplate;
        }

        public void setPulsarProperties(PulsarProperties pulsarProperties) {
            this.pulsarProperties = pulsarProperties;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
                DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableContext.getBeanFactory();

                Map<String, PulsarProperties.Producer> producerMap = pulsarProperties.getProducerMap();
                if (producerMap != null && !producerMap.isEmpty()) {
                    for (Map.Entry<String, PulsarProperties.Producer> entry : producerMap.entrySet()) {
                        String beanName = entry.getKey();
                        PulsarProperties.Producer config = entry.getValue();
                        beanFactory.registerBeanDefinition(beanName, BeanDefinitionBuilder.genericBeanDefinition(DefaultMultipleMessageSender.class, () -> {
                            DefaultMultipleMessageSender sender = new DefaultMultipleMessageSender();
                            sender.setPulsarTemplate(pulsarTemplate);
                            sender.setTopic(config.getTopic());
                            return sender;
                        }).getBeanDefinition());
                    }
                }
            }
        }
    }
}