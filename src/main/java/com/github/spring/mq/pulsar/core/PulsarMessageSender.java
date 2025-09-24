package com.github.spring.mq.pulsar.core;

import org.apache.pulsar.client.api.MessageId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar message sender interface
 *
 * <p>Provides a unified abstraction for message sending operations.
 * This interface defines various methods for sending messages to Pulsar topics
 * with different delivery modes and configurations.
 *
 * <p>Supported operations:
 * <ul>
 *   <li>Synchronous and asynchronous message sending</li>
 *   <li>Message sending with custom keys for partitioning</li>
 *   <li>Delayed message delivery</li>
 *   <li>Scheduled message delivery</li>
 *   <li>Transactional message sending</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public interface PulsarMessageSender {

    /**
     * Send message synchronously to default topic
     *
     * @param message Message content
     * @return Message ID
     */
    MessageId send(Object message);

    /**
     * Send message synchronously to specified topic
     *
     * @param topic   Topic name
     * @param message Message content
     * @return Message ID
     */
    MessageId send(String topic, Object message);

    /**
     * Send message synchronously to specified topic with message key
     *
     * @param topic   Topic name
     * @param key     Message key for partitioning
     * @param message Message content
     * @return Message ID
     */
    MessageId send(String topic, String key, Object message);

    /**
     * Send message asynchronously to default topic
     *
     * @param message Message content
     * @return Future containing message ID
     */
    CompletableFuture<MessageId> sendAsync(Object message);

    /**
     * Send message asynchronously to specified topic
     *
     * @param topic   Topic name
     * @param message Message content
     * @return Future containing message ID
     */
    CompletableFuture<MessageId> sendAsync(String topic, Object message);

    /**
     * Send message asynchronously to specified topic with message key
     *
     * @param topic   Topic name
     * @param key     Message key for partitioning
     * @param message Message content
     * @return Future containing message ID
     */
    CompletableFuture<MessageId> sendAsync(String topic, String key, Object message);

    /**
     * Send delayed message
     *
     * @param topic   Topic name
     * @param message Message content
     * @param delay   Delay time
     * @param unit    Time unit for delay
     * @return Message ID
     */
    MessageId sendAfter(String topic, Object message, long delay, TimeUnit unit);

    /**
     * Send message at specified timestamp
     *
     * @param topic     Topic name
     * @param message   Message content
     * @param timestamp Delivery timestamp
     * @return Message ID
     */
    MessageId sendAt(String topic, Object message, long timestamp);

    /**
     * Send transactional message
     *
     * @param topic   Topic name
     * @param message Message content
     * @return Message ID
     */
    MessageId sendInTransaction(String topic, Object message);
}