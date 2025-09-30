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

package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;

/**
 * Dead letter message processor
 *
 * <p>This class handles messages that have been moved to the dead letter queue
 * after exceeding the maximum retry attempts. It provides basic processing
 * and acknowledgment functionality for dead letter messages.
 *
 * <p>The processor logs the dead letter message content and acknowledges
 * the message to prevent it from being redelivered.
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DeadLetterMessageProcessor {
    private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(PulsarTemplate.class);

    /**
     * Process dead letter message
     */
    public void process(Consumer<byte[]> consumer, Message<byte[]> message) {
        try {
            byte[] data = message.getData();
            logger.info("Dead letter message: {}", new String(data));
            if (consumer != null && consumer.isConnected()) {
                consumer.acknowledge(message);
            }
        } catch (PulsarClientException e) {
            logger.error("Dead letter queue consumption exception", e);
        }
    }
}