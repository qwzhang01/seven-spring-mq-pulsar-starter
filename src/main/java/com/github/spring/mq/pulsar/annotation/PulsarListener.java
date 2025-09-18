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
     * 消息路由
     * <p>
     * 注解消息处理器方法，可以按照消息体中 routeKey 指定的字段，映射处理器方法
     */
    String msgRoute() default "";

    /**
     * 消息路由键，默认msgRoute
     * <p>
     * 同一个 topic 下，可能有多个细分业务类型
     * 使用 routeKey 来区分，解析的时候按照 routeKey 指定的字段，寻找对应的消息处理器
     */
    String routeKey() default "msgRoute";

    /**
     * 数据键，默认data
     * <p>
     * 消息实体有一个统一包装类，包装类包含消息的一些元信息
     * 消息本身会在 dataKey 字段中，解析的时候只需要解析 dataKey 指定的字段 即可
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