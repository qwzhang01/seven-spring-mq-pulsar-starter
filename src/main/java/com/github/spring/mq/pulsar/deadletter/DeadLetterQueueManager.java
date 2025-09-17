package com.github.spring.mq.pulsar.deadletter;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 死信队列管理器
 * 提供死信队列的监控、清理和自动处理功能
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DeadLetterQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueManager.class);

    private final PulsarTemplate pulsarTemplate;

    private final DeadLetterMessageProcessor deadLetterMessageProcessor;

    // 死信队列消费者缓存
    private final ConcurrentHashMap<String, Consumer<byte[]>> deadLetterConsumers = new ConcurrentHashMap<>();

    // 死信队列监控信息
    private final ConcurrentHashMap<String, DeadLetterQueueInfo> queueInfoMap = new ConcurrentHashMap<>();

    public DeadLetterQueueManager(PulsarTemplate pulsarTemplate, DeadLetterMessageProcessor deadLetterMessageProcessor) {
        this.pulsarTemplate = pulsarTemplate;
        this.deadLetterMessageProcessor = deadLetterMessageProcessor;
    }

    /**
     * 创建死信队列消费者
     *
     * @param deadLetterTopic 死信队列主题
     * @return 消费者
     */
    public Consumer<byte[]> createDeadLetterConsumer(String deadLetterTopic) {
        return deadLetterConsumers.computeIfAbsent(deadLetterTopic, topic -> {
            try {
                if (pulsarTemplate == null) {
                    logger.warn("PulsarTemplate is not available, cannot create dead letter consumer");
                    return null;
                }

                Consumer<byte[]> consumer = pulsarTemplate.createConsumer(topic, "dead-letter-manager-subscription");

                // 初始化队列信息
                DeadLetterQueueInfo queueInfo = new DeadLetterQueueInfo();
                queueInfo.setTopic(topic);
                queueInfo.setCreatedTime(LocalDateTime.now());
                queueInfo.setLastCheckTime(LocalDateTime.now());
                queueInfoMap.put(topic, queueInfo);

                logger.info("Dead letter consumer created for topic: {}", topic);
                return consumer;

            } catch (PulsarClientException e) {
                logger.error("Failed to create dead letter consumer for topic: {}", topic, e);
                return null;
            }
        });
    }

    /**
     * 监控死信队列
     *
     * @param deadLetterTopic 死信队列主题
     * @return 队列信息
     */
    public DeadLetterQueueInfo monitorDeadLetterQueue(String deadLetterTopic) {
        DeadLetterQueueInfo queueInfo = queueInfoMap.get(deadLetterTopic);
        if (queueInfo == null) {
            queueInfo = new DeadLetterQueueInfo();
            queueInfo.setTopic(deadLetterTopic);
            queueInfo.setCreatedTime(LocalDateTime.now());
            queueInfoMap.put(deadLetterTopic, queueInfo);
        }

        try {
            Consumer<byte[]> consumer = createDeadLetterConsumer(deadLetterTopic);
            if (consumer != null) {
                // 检查队列中的消息数量（这里是一个简化的实现）
                // 在实际生产环境中，您可能需要使用Pulsar的管理API来获取更准确的统计信息
                queueInfo.setLastCheckTime(LocalDateTime.now());
                queueInfo.setHealthy(true);
            } else {
                queueInfo.setHealthy(false);
            }

        } catch (Exception e) {
            logger.error("Failed to monitor dead letter queue: {}", deadLetterTopic, e);
            queueInfo.setHealthy(false);
            queueInfo.setLastError(e.getMessage());
        }

        return queueInfo;
    }

    /**
     * 从死信队列中读取消息
     *
     * @param deadLetterTopic 死信队列主题
     * @param maxMessages     最大消息数量
     * @return 死信消息列表
     */
    public List<DefaultDeadLetterQueueHandler.DeadLetterMessage> readDeadLetterMessages(
            String deadLetterTopic, int maxMessages) {

        List<DefaultDeadLetterQueueHandler.DeadLetterMessage> messages = new ArrayList<>();

        try {
            Consumer<byte[]> consumer = createDeadLetterConsumer(deadLetterTopic);
            if (consumer == null) {
                logger.warn("Cannot create consumer for dead letter topic: {}", deadLetterTopic);
                return messages;
            }

            for (int i = 0; i < maxMessages; i++) {
                try {
                    Message<byte[]> message = consumer.receive(1, TimeUnit.SECONDS);
                    if (message == null) {
                        break; // 没有更多消息
                    }

                    // 反序列化死信消息
                    DefaultDeadLetterQueueHandler.DeadLetterMessage deadLetterMessage =
                            deserializeDeadLetterMessage(message);

                    if (deadLetterMessage != null) {
                        messages.add(deadLetterMessage);
                    }

                    // 确认消息（从死信队列中移除）
                    consumer.acknowledge(message);

                } catch (PulsarClientException e) {
                    logger.error("Failed to receive message from dead letter queue: {}", deadLetterTopic, e);
                    break;
                }
            }

            logger.info("Read {} messages from dead letter queue: {}", messages.size(), deadLetterTopic);

        } catch (Exception e) {
            logger.error("Failed to read messages from dead letter queue: {}", deadLetterTopic, e);
        }

        return messages;
    }

    /**
     * 清理过期的死信消息
     *
     * @param deadLetterTopic 死信队列主题
     * @param expirationDays  过期天数
     * @return 清理的消息数量
     */
    public int cleanupExpiredMessages(String deadLetterTopic, int expirationDays) {
        int cleanedCount = 0;

        try {
            List<DefaultDeadLetterQueueHandler.DeadLetterMessage> messages =
                    readDeadLetterMessages(deadLetterTopic, 1000); // 批量读取

            LocalDateTime expirationTime = LocalDateTime.now().minusDays(expirationDays);

            for (DefaultDeadLetterQueueHandler.DeadLetterMessage message : messages) {
                if (message.getFailureTime() != null && message.getFailureTime().isBefore(expirationTime)) {
                    // 消息已过期，记录日志但不重新发送
                    logger.info("Expired dead letter message cleaned: messageId={}, failureTime={}",
                            message.getOriginalMessageId(), message.getFailureTime());
                    cleanedCount++;
                } else {
                    // 消息未过期，重新发送到死信队列
                    if (pulsarTemplate != null) {
                        pulsarTemplate.send(deadLetterTopic, message);
                    }
                }
            }

            logger.info("Cleaned {} expired messages from dead letter queue: {}", cleanedCount, deadLetterTopic);

        } catch (Exception e) {
            logger.error("Failed to cleanup expired messages from dead letter queue: {}", deadLetterTopic, e);
        }

        return cleanedCount;
    }

    /**
     * 定时监控所有死信队列
     */
    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void scheduledMonitoring() {
        logger.debug("Starting scheduled dead letter queue monitoring");

        for (String topic : queueInfoMap.keySet()) {
            try {
                monitorDeadLetterQueue(topic);
            } catch (Exception e) {
                logger.error("Error during scheduled monitoring of dead letter queue: {}", topic, e);
            }
        }

        logger.debug("Completed scheduled dead letter queue monitoring");
    }

    /**
     * 定时清理过期消息
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void scheduledCleanup() {
        logger.info("Starting scheduled dead letter queue cleanup");

        int totalCleaned = 0;
        for (String topic : queueInfoMap.keySet()) {
            try {
                int cleaned = cleanupExpiredMessages(topic, 7); // 清理7天前的消息
                totalCleaned += cleaned;
            } catch (Exception e) {
                logger.error("Error during scheduled cleanup of dead letter queue: {}", topic, e);
            }
        }

        logger.info("Completed scheduled dead letter queue cleanup, total cleaned: {}", totalCleaned);
    }

    /**
     * 获取所有死信队列信息
     *
     * @return 队列信息列表
     */
    public List<DeadLetterQueueInfo> getAllQueueInfo() {
        return new ArrayList<>(queueInfoMap.values());
    }

    /**
     * 关闭死信队列管理器
     */
    public void shutdown() {
        logger.info("Shutting down dead letter queue manager");

        for (Consumer<byte[]> consumer : deadLetterConsumers.values()) {
            try {
                consumer.close();
            } catch (PulsarClientException e) {
                logger.error("Error closing dead letter consumer", e);
            }
        }

        deadLetterConsumers.clear();
        queueInfoMap.clear();

        logger.info("Dead letter queue manager shutdown completed");
    }

    /**
     * 反序列化死信消息
     */
    private DefaultDeadLetterQueueHandler.DeadLetterMessage deserializeDeadLetterMessage(Message<byte[]> message) {
        try {
            if (pulsarTemplate != null) {
                return pulsarTemplate.deserialize(message.getData(), null,
                        DefaultDeadLetterQueueHandler.DeadLetterMessage.class);
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize dead letter message: {}", message.getMessageId(), e);
        }
        return null;
    }

    /**
     * 死信队列信息
     */
    public static class DeadLetterQueueInfo {
        private String topic;
        private LocalDateTime createdTime;
        private LocalDateTime lastCheckTime;
        private boolean healthy;
        private String lastError;
        private long messageCount;
        private long totalProcessedCount;

        // Getters and Setters
        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public LocalDateTime getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(LocalDateTime createdTime) {
            this.createdTime = createdTime;
        }

        public LocalDateTime getLastCheckTime() {
            return lastCheckTime;
        }

        public void setLastCheckTime(LocalDateTime lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }

        public long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }

        public long getTotalProcessedCount() {
            return totalProcessedCount;
        }

        public void setTotalProcessedCount(long totalProcessedCount) {
            this.totalProcessedCount = totalProcessedCount;
        }

        @Override
        public String toString() {
            return "DeadLetterQueueInfo{" +
                    "topic='" + topic + '\'' +
                    ", createdTime=" + createdTime +
                    ", lastCheckTime=" + lastCheckTime +
                    ", healthy=" + healthy +
                    ", messageCount=" + messageCount +
                    ", totalProcessedCount=" + totalProcessedCount +
                    '}';
        }
    }
}