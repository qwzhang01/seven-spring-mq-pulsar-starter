package com.github.spring.mq.pulsar.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
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
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final Log log = LogFactory.getLog(PulsarAutoConfiguration.class);
    private final PulsarProperties pulsarProperties;

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
            ClientBuilder clientBuilder = PulsarClient.builder().serviceUrl(pulsarProperties.getServiceUrl()).operationTimeout((int) pulsarProperties.getClient().getOperationTimeout().toMillis(), TimeUnit.MILLISECONDS).connectionTimeout((int) pulsarProperties.getClient().getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
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
    public PulsarTemplate pulsarTemplate(PulsarClient pulsarClient, ObjectMapper objectMapper, PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry) {
        PulsarTemplate template = new PulsarTemplate(pulsarClient, pulsarProperties, objectMapper);
        template.setInterceptorRegistry(interceptorRegistry);
        return template;
    }

    /**
     * 创建监听器容器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerContainerFactory pulsarListenerContainerFactory(PulsarTemplate pulsarTemplate) {
        return new PulsarListenerContainerFactory(pulsarProperties, pulsarTemplate);
    }

    /**
     * 创建监听器注解处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerAnnotationBeanPostProcessor pulsarListenerAnnotationBeanPostProcessor(PulsarListenerContainerFactory containerFactory) {
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

    @Primary
    @Bean("pulsarObjectMapper")
    public ObjectMapper pulsarObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        //不区分大小写设置
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        //对象的所有字段全部列入
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        //忽略空Bean转json的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //取消默认转换timestamps形式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        //忽略在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT));

        // 默认启用，通常无需显式设置
        objectMapper.enable(MapperFeature.USE_ANNOTATIONS);

        // double json 格式化
        SimpleModule doubleModule = new SimpleModule();
        doubleModule.addSerializer(Double.class, DoubleFormatSerializer.INSTANCE);
        objectMapper.registerModule(doubleModule);

        return objectMapper;
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

    /**
     * double json 格式化
     * 保留2位小数
     *
     * @author avinzhang
     */
    private static final class DoubleFormatSerializer extends JsonSerializer<Double> {

        private static final DoubleFormatSerializer INSTANCE = new DoubleFormatSerializer();
        private static final DecimalFormat FORMAT = new DecimalFormat("###.##");

        @Override
        public void serialize(Double value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            String text = null;
            if (value != null) {
                try {
                    text = FORMAT.format(value);
                } catch (Exception e) {
                    text = value.toString();
                }
            }
            if (text != null) {
                jsonGenerator.writeString(text);
            }
        }
    }
}