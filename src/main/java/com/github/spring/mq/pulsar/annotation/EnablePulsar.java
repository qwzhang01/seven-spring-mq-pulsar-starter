package com.github.spring.mq.pulsar.annotation;

import com.github.spring.mq.pulsar.config.PulsarConfigurationSelector;
import com.github.spring.mq.pulsar.domain.ListenerType;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Pulsar 功能的注解
 *
 * <p>使用此注解可以启用 Pulsar 的自动配置功能，包括：
 * <ul>
 *   <li>Pulsar 客户端自动配置</li>
 *   <li>消息生产者和消费者</li>
 *   <li>监听器容器</li>
 *   <li>事务支持</li>
 *   <li>健康检查</li>
 * </ul>
 *
 * <p>示例用法：
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
     * 是否启用 Pulsar 功能
     * 默认为 true，可以通过 spring.pulsar.enabled 配置项覆盖
     */
    boolean enabled() default true;

    /**
     * 是否启用事务支持
     * 默认为 false，启用后可以使用 @Transactional 注解
     */
    boolean enableTransaction() default false;

    /**
     * 是否启用健康检查
     * 默认为 true，会注册 Pulsar 健康检查端点
     */
    boolean enableHealthCheck() default true;

    /**
     * 是否启用消息拦截器
     * 默认为 true，可以注册自定义的消息拦截器
     */
    boolean enableInterceptor() default true;

    /**
     * 是否启用默认日志拦截器
     */
    boolean enableLogInterceptor() default false;

    /**
     * 是否启用默认性能拦截器
     */
    boolean enablePerformanceInterceptor() default false;

    /**
     * 监听器类型
     * <p>
     * 默认使用 监听器事件模式
     */
    ListenerType listenerType() default ListenerType.LOOP;
}