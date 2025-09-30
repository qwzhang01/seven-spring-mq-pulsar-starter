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
