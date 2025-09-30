/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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