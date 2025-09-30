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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging Pulsar message interceptor example
 *
 * <p>Demonstrates how to implement a custom Pulsar message interceptor
 * for logging purposes. This interceptor logs message send and receive
 * operations with detailed information including:
 * <ul>
 *   <li>Message content and topic information</li>
 *   <li>Send/receive timestamps</li>
 *   <li>Success/failure status</li>
 *   <li>Error details when exceptions occur</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class LoggingPulsarMessageInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingPulsarMessageInterceptor.class);
    private final ObjectMapper objectMapper;

    public LoggingPulsarMessageInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object beforeSend(String topic, Object message) {
        String logMsg = "";
        try {
            logMsg = message instanceof String ? (String) message : objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new com.github.spring.mq.pulsar.exception.JacksonException("", e);
        }
        logger.info("Preparing to send message to topic: {}, message content: {}", topic, logMsg);
        // Message preprocessing can be done here
        // For example: adding timestamps, encryption, validation, etc.

        // Return processed message
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        if (exception == null) {
            logger.info("Message sent successfully - Topic: {}, Message ID: {}", topic, messageId);
        } else {
            logger.error("Message send failed - Topic: {}, Error: {}", topic, exception.getMessage(), exception);
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        logger.info("Preparing to receive message - Topic: {}, Message ID: {}, Publish time: {}",
                message.getTopicName(), message.getMessageId(), message.getPublishTime());

        // Message filtering can be done here
        // Returning false will skip processing of this message

        // Continue processing message
        return true;
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        if (exception == null) {
            logger.info("Message processed successfully - Topic: {}, Message ID: {}",
                    message.getTopicName(), message.getMessageId());
        } else {
            logger.error("Message processing failed - Topic: {}, Message ID: {}, Error: {}",
                    message.getTopicName(), message.getMessageId(), exception.getMessage(), exception);
        }
    }

    @Override
    public int getOrder() {
        // Set interceptor priority, lower values indicate higher priority
        return 100;
    }
}