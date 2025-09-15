package com.github.spring.mq.pulsar.annotation;

import java.lang.annotation.*;

/**
 * Pulsar 监听器注解
 *
 * @author avinzhang
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PulsarListener {

    /**
     * 主题名称
     */
    String topic() default "";

    /**
     * 业务路径
     * 同一个消息可能存在多种业务
     */
    String businessPath() default "";

    /**
     * 指定消息体中的业务路径键
     * 默认 businessPath
     */
    String businessKey() default "businessPath";

    /**
     * 数据键
     * 默认data
     */
    String dataKey() default "data";

    /**
     * 消费者名称
     * 标识消费者实例，在分布式系统中做日志标识，不对消息消费产生影响
     */
    String consumerName() default "";

    /**
     * 消息类型
     */
    Class<?> messageType() default String.class;
}