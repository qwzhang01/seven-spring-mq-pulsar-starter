package com.github.spring.mq.pulsar.annotation;

import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;

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
     * 指定消息体中的业务路径字段
     */
    String businessKey() default "businessPath";

    String retryTopic() default "";

    String deadTopic() default "";

    /**
     * 订阅名称
     */
    String subscription() default "";

    /**
     * 订阅类型
     *
     * @see SubscriptionType
     */
    String subscriptionType() default "Exclusive";

    /**
     * 订阅初始位置
     *
     * @see SubscriptionInitialPosition
     */
    String subscriptionInitialPosition() default "Earliest";

    /**
     * 消费者名称
     * 标识消费者实例，在分布式系统中做日志标识，不对消息消费产生影响
     */
    String consumerName() default "";

    /**
     * Sets the size of the consumer receive queue.
     */
    int receiverQueueSize() default 1000;

    /**
     * 没有Ack的消息，默认10秒后重新消费
     */
    int ackTimeout() default 10000;

    /**
     * 重试次数
     */
    int retryTime() default 3;

    /**
     * negativeAck的消息，重新消费延迟时间
     * 默认1000毫秒
     */
    int negativeAckRedeliveryDelay() default 1000;

    /**
     * 消息重新消费延迟时间
     */
    int timeToReconsumeDelay() default 10000;

    /**
     * 是否自动确认消息
     */
    boolean autoAck() default true;

    /**
     * 消息类型
     */
    Class<?> messageType() default String.class;
}