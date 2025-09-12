package com.github.spring.mq.pulsar.deadletter;

import org.apache.pulsar.client.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认死信队列处理器实现
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultDeadLetterQueueHandler implements DeadLetterQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDeadLetterQueueHandler.class);

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        String deadLetterTopic = getDeadLetterTopic(originalTopic);

        logger.error("Message sent to dead letter queue. Original topic: {}, Dead letter topic: {}, Message ID: {}",
                originalTopic, deadLetterTopic, message.getMessageId(), exception);

        // TODO: 实现将消息发送到死信队列的逻辑
        // 这里可以使用 PulsarTemplate 将消息重新发送到死信队列主题
    }

    @Override
    public boolean shouldSendToDeadLetter(Message<?> message, Exception exception, int retryCount) {
        // 检查是否是不可重试的异常
        if (isNonRetryableException(exception)) {
            return true;
        }

        return retryCount >= getMaxRetries();
    }

    /**
     * 判断是否是不可重试的异常
     *
     * @param exception 异常
     * @return 是否不可重试
     */
    private boolean isNonRetryableException(Exception exception) {
        // 这里可以定义哪些异常不需要重试，直接发送到死信队列
        return exception instanceof IllegalArgumentException ||
                exception instanceof SecurityException;
    }
}