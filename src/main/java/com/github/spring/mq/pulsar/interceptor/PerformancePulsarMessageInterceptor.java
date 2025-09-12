package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 性能监控拦截器
 *
 * @author avinzhang
 */
public class PerformancePulsarMessageInterceptor implements PulsarMessageInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(PerformancePulsarMessageInterceptor.class);

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public Object beforeSend(String topic, Object message) {
        startTime.set(System.currentTimeMillis());
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            logger.info("消息发送耗时: " + duration + "ms, 主题: " + topic);
            startTime.remove();
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        startTime.set(System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            logger.info("消息处理耗时: " + duration + "ms, 主题: " + message.getTopicName());
            startTime.remove();
        }
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保能准确测量时间
        return 10;
    }
}
