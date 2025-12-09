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

package com.github.spring.mq.pulsar.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.github.spring.mq.pulsar.core.DefaultPulsarMessageSender;
import com.github.spring.mq.pulsar.core.DefaultTopicMessageSender;
import com.github.spring.mq.pulsar.core.PulsarMessageSender;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.exception.PulsarClientInitException;
import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerAnnotationBeanPostProcessor;
import com.github.spring.mq.pulsar.listener.PulsarListenerContainerFactory;
import io.micrometer.tracing.Tracer;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar auto-configuration class
 *
 * <p>This class provides auto-configuration for Pulsar integration, including:
 * <ul>
 *   <li>Pulsar client configuration</li>
 *   <li>Message template configuration</li>
 *   <li>Producer and consumer setup</li>
 *   <li>Object mapper configuration</li>
 *   <li>Listener annotation processing</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(PulsarClient.class)
@EnableConfigurationProperties(PulsarProperties.class)
@ConditionalOnProperty(name = "spring.pulsar.enabled", havingValue = "true", matchIfMissing = true)
public class PulsarAutoConfiguration {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final Log log = LogFactory.getLog(PulsarAutoConfiguration.class);
    private final PulsarProperties pulsarProperties;

    public PulsarAutoConfiguration(PulsarProperties pulsarProperties) {
        this.pulsarProperties = pulsarProperties;
    }

    /**
     * Create Pulsar client
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarClient pulsarClient() throws PulsarClientException {
        // Validate configuration parameters
        pulsarProperties.valid();
        // Initialize Pulsar client
        try {
            ClientBuilder clientBuilder = PulsarClient.builder().serviceUrl(pulsarProperties.getServiceUrl())
                    .operationTimeout((int) pulsarProperties.getClient().getOperationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .connectionTimeout((int) pulsarProperties.getClient().getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    // Netty's ioThreads are responsible for network IO operations, 
                    // if business traffic is large, you can increase the number of ioThreads
                    .ioThreads(pulsarProperties.getClient().getNumIoThreads())
                    // Responsible for calling callback functions of consumers started in listener mode,
                    // it is recommended to configure more than the number of partitions that this client is responsible for
                    .listenerThreads(pulsarProperties.getClient().getNumListenerThreads());

            // Configure authentication
            PulsarProperties.Authentication auth = pulsarProperties.getAuthentication();
            if (auth.isEnabled()) {
                if (StringUtils.hasText(auth.getToken())) {
                    clientBuilder.authentication("org.apache.pulsar.client.impl.auth.AuthenticationToken",
                            auth.getToken());
                } else if (StringUtils.hasText(auth.getAuthPluginClassName())) {
                    clientBuilder.authentication(auth.getAuthPluginClassName(),
                            auth.getAuthParams());
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
     * Create Pulsar template
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public PulsarTemplate pulsarTemplate(PulsarClient pulsarClient,
                                         ObjectMapper objectMapper,
                                         PulsarInterceptorConfiguration.PulsarInterceptorRegistry interceptorRegistry,
                                         DeadLetterMessageProcessor deadLetterMessageProcessor,
                                         Tracer tracer) {
        PulsarTemplate template = new PulsarTemplate(pulsarClient, pulsarProperties, objectMapper, deadLetterMessageProcessor, tracer);
        template.setInterceptorRegistry(interceptorRegistry);
        return template;
    }

    /**
     * Create message sender
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarMessageSender pulsarMessageSender(PulsarTemplate pulsarTemplate) {
        return new DefaultPulsarMessageSender(pulsarTemplate, pulsarProperties);
    }

    /**
     * Create multiple producer bean registrar
     */
    @Bean
    @ConditionalOnMissingBean
    public MultipleProducerBeanRegistrar multipleProducerBeanRegistrar(PulsarMessageSender pulsarMessageSender) {
        MultipleProducerBeanRegistrar multipleProducerBeanRegistrar = new MultipleProducerBeanRegistrar();
        multipleProducerBeanRegistrar.setPulsarProperties(pulsarProperties);
        multipleProducerBeanRegistrar.setPulsarMessageSender(pulsarMessageSender);
        return multipleProducerBeanRegistrar;
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        SimpleModule doubleModule = new SimpleModule();
        doubleModule.addSerializer(Double.class, DoubleFormatSerializer.INSTANCE);

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        // Configure LocalDateTime serialization and deserialization
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        // Configure LocalDate serialization and deserialization
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        return JsonMapper.builder()
                // Case insensitive property mapping
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                // Include all object fields
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.ALWAYS, JsonInclude.Include.NON_NULL))
                // Ignore empty Bean to JSON conversion errors
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                // Disable default timestamp conversion
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Ignore properties that exist in JSON but not in Java object to prevent errors
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .defaultDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT))
                // Enabled by default, usually no need to set explicitly
                .enable(MapperFeature.USE_ANNOTATIONS)
                .build()
                // Double JSON formatting
                .registerModule(doubleModule)
                .registerModule(javaTimeModule);
    }

    /**
     * Create listener annotation processor
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarListenerAnnotationBeanPostProcessor pulsarListenerAnnotationBeanPostProcessor(PulsarListenerContainerFactory containerFactory) {
        return new PulsarListenerAnnotationBeanPostProcessor(containerFactory);
    }

    /**
     * Multiple producer bean registrar
     */
    public static class MultipleProducerBeanRegistrar implements ApplicationContextAware, InitializingBean {

        private ApplicationContext applicationContext;
        private PulsarProperties pulsarProperties;
        private PulsarMessageSender pulsarMessageSender;

        public void setPulsarMessageSender(PulsarMessageSender pulsarMessageSender) {
            this.pulsarMessageSender = pulsarMessageSender;
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
                        beanFactory.registerBeanDefinition(beanName, BeanDefinitionBuilder.genericBeanDefinition(DefaultTopicMessageSender.class, () -> {
                            DefaultTopicMessageSender sender = new DefaultTopicMessageSender();
                            sender.setPulsarMessageSender(pulsarMessageSender);
                            sender.setTopic(config.getTopic());
                            return sender;
                        }).getBeanDefinition());
                    }
                }
            }
        }
    }

    /**
     * Double JSON formatting
     * Keep 2 decimal places
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