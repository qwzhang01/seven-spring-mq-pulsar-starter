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

import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.exception.PulsarProducerSendException;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of Pulsar message sender
 *
 * <p>This class provides a concrete implementation of the {@link PulsarMessageSender} interface,
 * offering various methods for sending messages to Pulsar topics. It supports:
 * <ul>
 *   <li>Synchronous and asynchronous message sending</li>
 *   <li>Delayed message delivery</li>
 *   <li>Scheduled message delivery</li>
 *   <li>Message sending with custom keys</li>
 *   <li>Transaction message support (planned)</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultPulsarMessageSender implements PulsarMessageSender {

    private final PulsarTemplate pulsarTemplate;
    private final PulsarProperties pulsarProperties;

    public DefaultPulsarMessageSender(PulsarTemplate pulsarTemplate, PulsarProperties pulsarProperties) {
        this.pulsarTemplate = pulsarTemplate;
        this.pulsarProperties = pulsarProperties;
    }

    @Override
    public MessageId send(Object message) {
        String defaultTopic = pulsarProperties.getProducer().getTopic();
        if (!StringUtils.hasText(defaultTopic)) {
            throw new IllegalStateException("Default topic is not configured");
        }
        try {
            return pulsarTemplate.send(defaultTopic, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message", e);
        }
    }

    @Override
    public MessageId send(String topic, Object message) {
        try {
            return pulsarTemplate.send(topic, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public MessageId send(String topic, String key, Object message) {
        try {
            return pulsarTemplate.send(topic, key, message);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(Object message) {
        String defaultTopic = pulsarProperties.getProducer().getTopic();
        if (!StringUtils.hasText(defaultTopic)) {
            CompletableFuture<MessageId> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Default topic is not configured"));
            return future;
        }
        return pulsarTemplate.sendAsync(defaultTopic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String topic, Object message) {
        return pulsarTemplate.sendAsync(topic, message);
    }

    @Override
    public CompletableFuture<MessageId> sendAsync(String topic, String key, Object message) {
        return pulsarTemplate.sendAsync(topic, key, message);
    }

    @Override
    public MessageId sendAfter(String topic, Object message, long delay, TimeUnit unit) {
        try {
            return pulsarTemplate.sendAfter(topic, message, delay, unit);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public MessageId sendAt(String topic, Object message, long timestamp) {
        try {
            return pulsarTemplate.sendAt(topic, message, timestamp);
        } catch (PulsarClientException e) {
            throw new PulsarProducerSendException("Failed to send message to topic: " + topic, e);
        }
    }

    @Override
    public MessageId sendInTransaction(String topic, Object message) {
        // TODO: Implement transaction message sending
        throw new UnsupportedOperationException("Transaction message not implemented yet");
    }
}