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