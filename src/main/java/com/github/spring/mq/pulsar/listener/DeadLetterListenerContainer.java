package com.github.spring.mq.pulsar.listener;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 死信队列监听器容器
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
     * 启动监听器
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
     * 停止监听器
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
