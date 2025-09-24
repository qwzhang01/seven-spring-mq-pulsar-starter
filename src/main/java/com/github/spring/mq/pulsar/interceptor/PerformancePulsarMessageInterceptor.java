package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance monitoring interceptor
 *
 * <p>This interceptor measures the time taken for message sending and processing
 * operations. It provides performance metrics by logging the duration of:
 * <ul>
 *   <li>Message send operations</li>
 *   <li>Message receive and processing operations</li>
 * </ul>
 *
 * <p>The interceptor uses ThreadLocal to ensure accurate timing measurements
 * in multi-threaded environments and has the highest priority to ensure
 * accurate time measurement.
 *
 * @author avinzhang
 * @since 1.0.0
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
            logger.info("Message send duration: {}ms, Topic: {}", duration, topic);
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
            logger.info("Message processing duration: {}ms, Topic: {}", duration, message.getTopicName());
            startTime.remove();
        }
    }

    @Override
    public int getOrder() {
        // Highest priority to ensure accurate time measurement
        return 10;
    }
}
