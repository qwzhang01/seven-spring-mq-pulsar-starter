package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.annotation.EnablePulsar;
import com.github.spring.mq.pulsar.domain.ListenerType;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulsar 配置选择器
 * 根据 @EnablePulsar 注解的配置动态导入相关配置类
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarConfigurationSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnablePulsar.class.getName()));

        if (attributes == null) {
            return new String[0];
        }
        boolean enabled = attributes.getBoolean("enabled");
        if (!Boolean.TRUE.equals(enabled)) {
            return new String[0];
        }

        List<String> imports = new ArrayList<>();

        // 基础配置始终导入
        imports.add(PulsarAutoConfiguration.class.getName());

        // 消费者配置
        if (ListenerType.EVENT.equals(attributes.getEnum("listenerType"))) {
            imports.add(PulsarConsumerEventConfiguration.class.getName());
        }
        if (ListenerType.LOOP.equals(attributes.getEnum("listenerType"))) {
            imports.add(PulsarConsumerLoopConfiguration.class.getName());
        }
        imports.add(PulsarDeadLetterConfiguration.class.getName());

        // 根据注解配置导入相应的配置类
        if (attributes.getBoolean("enableTransaction")) {
            imports.add(PulsarTransactionConfiguration.class.getName());
        }

        if (attributes.getBoolean("enableHealthCheck")) {
            imports.add(PulsarHealthConfiguration.class.getName());
        }

        if (attributes.getBoolean("enableInterceptor")) {
            imports.add(PulsarInterceptorConfiguration.class.getName());
        }

        if (attributes.getBoolean("enableLogInterceptor")) {
            imports.add(PulsarLogInterceptorConfiguration.class.getName());
        }

        if (attributes.getBoolean("enablePerformanceInterceptor")) {
            imports.add(PulsarPerformanceInterceptorConfiguration.class.getName());
        }

        // 链路追踪配置 - 默认启用
        imports.add(PulsarTracingConfiguration.class.getName());

        return imports.toArray(new String[0]);
    }
}