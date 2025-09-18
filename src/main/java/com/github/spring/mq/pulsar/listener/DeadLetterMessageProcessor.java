package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.logging.log4j.Logger;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 死信消息处理器
 *
 * @author avinzhang
 */
public class DeadLetterMessageProcessor {
    private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(PulsarTemplate.class);

    /*** 处理死信消息 */
    public void process(Consumer<byte[]> consumer, CompletableFuture<Messages<byte[]>> future) {
        try {
            Messages<byte[]> messages = future.get();
            for (Message<byte[]> message : messages) {
                byte[] data = message.getData();
                logger.info("死信消息: {}", new String(data));
                consumer.acknowledge(message);
            }
        } catch (ExecutionException | InterruptedException | PulsarClientException e) {
            logger.error("死信队列消费异常", e);
        }
    }
}
