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