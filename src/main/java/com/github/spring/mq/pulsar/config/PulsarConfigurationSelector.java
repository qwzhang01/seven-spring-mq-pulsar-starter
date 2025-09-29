package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.annotation.EnablePulsar;
import com.github.spring.mq.pulsar.domain.ListenerType;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Pulsar configuration selector
 *
 * <p>Dynamically imports related configuration classes based on the configuration
 * of the @EnablePulsar annotation. This selector analyzes the annotation attributes
 * and conditionally imports the appropriate configuration classes.
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

        // Always import basic configuration
        imports.add(PulsarAutoConfiguration.class.getName());

        // Consumer configuration
        if (ListenerType.EVENT.equals(attributes.getEnum("listenerType"))) {
            imports.add(PulsarConsumerEventConfiguration.class.getName());
        }
        if (ListenerType.LOOP.equals(attributes.getEnum("listenerType"))) {
            imports.add(PulsarConsumerLoopConfiguration.class.getName());
        }
        imports.add(PulsarDeadLetterConfiguration.class.getName());

        // Import corresponding configuration classes based on annotation configuration
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

        // Tracing configuration - enabled by default
        imports.add(PulsarTracingConfiguration.class.getName());

        return imports.toArray(new String[0]);
    }
}