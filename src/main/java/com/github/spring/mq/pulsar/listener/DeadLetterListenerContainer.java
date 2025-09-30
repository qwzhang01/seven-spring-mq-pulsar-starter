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

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dead letter queue listener container
 *
 * @author avinzhang
 */
public class DeadLetterListenerContainer {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterListenerContainer.class);
    private final Consumer<byte[]> consumer;

    private final DeadLetterMessageProcessor deadLetterMessageProcessor;

    private final ExecutorService executor;
    private volatile boolean running = false;

    public DeadLetterListenerContainer(Consumer<byte[]> consumer, DeadLetterMessageProcessor deadLetterMessageProcessor) {
        this.consumer = consumer;
        this.deadLetterMessageProcessor = deadLetterMessageProcessor;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "pulsar-deadletter-listener-" + consumer.toString());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start listener
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::listen);
    }

    private void listen() {
        while (running) {
            try {
                Message<byte[]> message = consumer.receive();
                deadLetterMessageProcessor.process(consumer, message);
            } catch (Exception e) {
                logger.error("Error receiving message", e);
            }
        }
    }

    /**
     * Stop listener
     */
    public void stop() {
        running = false;
        this.executor.shutdown();
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            logger.error("Error closing consumer", e);
        }
    }
}
