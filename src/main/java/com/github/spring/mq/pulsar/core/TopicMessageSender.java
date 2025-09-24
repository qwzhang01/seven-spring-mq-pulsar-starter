package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Multi-producer Pulsar message sender interface
 *
 * <p>Topic-based message sender that provides a convenient abstraction
 * for sending messages to a specific topic. This interface is designed
 * for scenarios where multiple producers are needed for different topics.
 *
 * <p>Features:
 * <ul>
 *   <li>Topic-specific message sending</li>
 *   <li>Message routing support</li>
 *   <li>Synchronous and asynchronous operations</li>
 *   <li>Delayed and scheduled message delivery</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface TopicMessageSender {
    /**
     * Inject PulsarMessageSender
     */
    void setPulsarMessageSender(PulsarMessageSender pulsarMessageSender);

    /**
     * Set topic name
     */
    void setTopic(String topic);

    /**
     * Send message
     */
    MessageId send(Object message);

    MessageId send(Object message, String msgRoute);

    /**
     * Send message with key
     */
    MessageId send(String key, Object message);

    MessageId send(String key, Object message, String msgRoute);

    /**
     * Send message asynchronously
     */
    CompletableFuture<MessageId> sendAsync(Object message);

    CompletableFuture<MessageId> sendAsync(Object message, String msgRoute);

    /**
     * Send message asynchronously with key
     */
    CompletableFuture<MessageId> sendAsync(String key, Object message);

    CompletableFuture<MessageId> sendAsync(String key, Object message, String msgRoute);

    /**
     * Send delayed message
     */
    MessageId sendAfter(Object message, long delay, TimeUnit unit);

    MessageId sendAfter(Object message, String msgRoute, long delay, TimeUnit unit);

    /**
     * Send message at specified timestamp
     */
    MessageId sendAt(Object message, long timestamp);

    MessageId sendAt(Object message, String msgRoute, long timestamp);
}