package com.github.spring.mq.pulsar.annotation;

import com.github.spring.mq.pulsar.config.PulsarConfigurationSelector;
import com.github.spring.mq.pulsar.domain.ListenerType;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation to enable Pulsar functionality
 *
 * <p>Using this annotation enables Pulsar auto-configuration features, including:
 * <ul>
 *   <li>Pulsar client auto-configuration</li>
 *   <li>Message producers and consumers</li>
 *   <li>Listener containers</li>
 *   <li>Transaction support</li>
 *   <li>Health checks</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnablePulsar
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PulsarConfigurationSelector.class)
public @interface EnablePulsar {

    /**
     * Whether to enable Pulsar functionality
     * Default is true, can be overridden by spring.pulsar.enabled configuration
     */
    boolean enabled() default true;

    /**
     * Whether to enable transaction support
     * Default is false, when enabled, @Transactional annotation can be used
     */
    boolean enableTransaction() default false;

    /**
     * Whether to enable health checks
     * Default is true, will register Pulsar health check endpoints
     */
    boolean enableHealthCheck() default true;

    /**
     * Whether to enable message interceptors
     * Default is true, allows registration of custom message interceptors
     */
    boolean enableInterceptor() default true;

    /**
     * Whether to enable default logging interceptor
     */
    boolean enableLogInterceptor() default false;

    /**
     * Whether to enable default performance interceptor
     */
    boolean enablePerformanceInterceptor() default false;

    /**
     * Listener type
     * <p>
     * Default uses listener event mode
     */
    ListenerType listenerType() default ListenerType.LOOP;
}