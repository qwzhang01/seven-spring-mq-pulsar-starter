package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;

/**
 * Pulsar message interceptor interface
 * 
 * <p>This interface allows intercepting and processing messages during the send and receive process.
 * Interceptors can be used for various purposes such as:
 * <ul>
 *   <li>Message transformation and validation</li>
 *   <li>Logging and monitoring</li>
 *   <li>Security and authentication</li>
 *   <li>Message filtering and routing</li>
 *   <li>Error handling and retry logic</li>
 * </ul>
 * 
 * <p>Interceptors are executed in order based on their priority (lower values = higher priority).
 * Multiple interceptors can be chained together to form a processing pipeline.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface PulsarMessageInterceptor {

    /**
     * Intercept before sending message
     *
     * @param topic   Topic name
     * @param message Message content
     * @return Processed message content, return null to skip sending
     */
    default Object beforeSend(String topic, Object message) {
        return message;
    }

    /**
     * Intercept after sending message
     *
     * @param topic     Topic name
     * @param message   Message content
     * @param messageId Message ID
     * @param exception Send exception (if any)
     */
    default void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        // Default empty implementation
    }

    /**
     * Intercept before receiving message
     *
     * @param message Original message
     * @return Whether to continue processing the message
     */
    default boolean beforeReceive(Message<?> message) {
        return true;
    }

    /**
     * Intercept after receiving message
     *
     * @param message          Original message
     * @param processedMessage Processed message content
     * @param exception        Processing exception (if any)
     */
    default void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        // Default empty implementation
    }

    /**
     * Get interceptor priority
     * Lower values indicate higher priority
     *
     * @return Priority value
     */
    default int getOrder() {
        return 0;
    }
}