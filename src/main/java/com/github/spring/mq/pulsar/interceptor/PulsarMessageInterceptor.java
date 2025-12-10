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

package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.TypedMessageBuilder;

/**
 * Pulsar message interceptor interface
 *
 * <p>Provides interception points for Pulsar message processing lifecycle:
 * <ul>
 *   <li>Message sending preparation</li>
 *   <li>Pre-send interception</li>
 *   <li>Post-send notification</li>
 *   <li>Pre-receive interception</li>
 *   <li>Post-receive processing</li>
 * </ul>
 *
 * <p>Implementations can use these interception points to:
 * <ul>
 *   <li>Enrich messages with metadata</li>
 *   <li>Propagate tracing context</li>
 *   <li>Handle tenant isolation</li>
 *   <li>Implement custom error handling</li>
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
     * Intercept message builder
     * <p>
     * 构建消息的时候拦截，补偿消息构建逻辑
     *
     * <p>This method is called during message preparation phase, allowing
     * implementations to modify the message builder before the message
     * is finalized and sent.
     *
     * <p>Typical use cases include:
     * <ul>
     *   <li>Adding custom message properties</li>
     *   <li>Injecting tracing context headers</li>
     *   <li>Setting message delivery semantics</li>
     * </ul>
     *
     * @param messageBuilder the message builder being prepared
     */
    default void messageBuilder(TypedMessageBuilder<byte[]> messageBuilder) {
        // Default empty implementation
    }

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