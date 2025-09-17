package com.github.spring.mq.pulsar.deadletter;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 死信消息处理器
 * 提供死信消息的重新处理、统计和管理功能
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DeadLetterMessageProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterMessageProcessor.class);

    private final PulsarTemplate pulsarTemplate;

    // 死信消息统计
    private final AtomicLong totalDeadLetterCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> topicDeadLetterCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastProcessTime = new ConcurrentHashMap<>();

    public DeadLetterMessageProcessor(PulsarTemplate pulsarTemplate) {
        this.pulsarTemplate = pulsarTemplate;
    }

    /**
     * 重新处理死信消息
     * 将死信消息重新发送到原始主题
     *
     * @param deadLetterMessage 死信消息
     * @param targetTopic       目标主题（如果为null则使用原始主题）
     * @return 是否处理成功
     */
    public boolean reprocessDeadLetterMessage(DefaultDeadLetterQueueHandler.DeadLetterMessage deadLetterMessage, String targetTopic) {
        try {
            if (deadLetterMessage == null) {
                logger.warn("Dead letter message is null, cannot reprocess");
                return false;
            }

            String topic = targetTopic != null ? targetTopic : deadLetterMessage.getOriginalTopic();
            if (topic == null) {
                logger.warn("Target topic is null, cannot reprocess dead letter message: {}",
                        deadLetterMessage.getOriginalMessageId());
                return false;
            }

            if (pulsarTemplate == null) {
                logger.warn("PulsarTemplate is not available, cannot reprocess dead letter message");
                return false;
            }

            // 重新构建消息内容
            ReprocessedMessage reprocessedMessage = buildReprocessedMessage(deadLetterMessage);

            // 发送到目标主题
            MessageId messageId = pulsarTemplate.send(topic, deadLetterMessage.getOriginalKey(), reprocessedMessage);

            logger.info("Dead letter message reprocessed successfully: originalMessageId={}, newMessageId={}, topic={}",
                    deadLetterMessage.getOriginalMessageId(), messageId, topic);

            // 更新统计信息
            updateReprocessStatistics(topic);

            return true;

        } catch (Exception e) {
            logger.error("Failed to reprocess dead letter message: originalMessageId={}",
                    deadLetterMessage.getOriginalMessageId(), e);
            return false;
        }
    }

    /**
     * 批量重新处理死信消息
     *
     * @param deadLetterMessages 死信消息列表
     * @param targetTopic        目标主题
     * @return 处理结果统计
     */
    public BatchReprocessResult batchReprocessDeadLetterMessages(
            List<DefaultDeadLetterQueueHandler.DeadLetterMessage> deadLetterMessages,
            String targetTopic) {

        BatchReprocessResult result = new BatchReprocessResult();
        result.setTotalCount(deadLetterMessages.size());
        result.setStartTime(LocalDateTime.now());

        CompletableFuture<Void>[] futures = new CompletableFuture[deadLetterMessages.size()];

        for (int i = 0; i < deadLetterMessages.size(); i++) {
            final DefaultDeadLetterQueueHandler.DeadLetterMessage message = deadLetterMessages.get(i);
            futures[i] = CompletableFuture.runAsync(() -> {
                boolean success = reprocessDeadLetterMessage(message, targetTopic);
                if (success) {
                    result.incrementSuccessCount();
                } else {
                    result.incrementFailureCount();
                    result.addFailedMessage(message.getOriginalMessageId());
                }
            });
        }

        try {
            // 等待所有任务完成
            CompletableFuture.allOf(futures).get();
        } catch (Exception e) {
            logger.error("Error during batch reprocessing", e);
        }

        result.setEndTime(LocalDateTime.now());

        logger.info("Batch reprocess completed: total={}, success={}, failure={}, duration={}ms",
                result.getTotalCount(), result.getSuccessCount(), result.getFailureCount(),
                result.getDurationMillis());

        return result;
    }

    /**
     * 获取死信消息统计信息
     *
     * @return 统计信息
     */
    public DeadLetterStatistics getStatistics() {
        DeadLetterStatistics statistics = new DeadLetterStatistics();
        statistics.setTotalDeadLetterCount(totalDeadLetterCount.get());
        statistics.setTopicDeadLetterCount(new ConcurrentHashMap<>(topicDeadLetterCount));
        statistics.setLastProcessTime(new ConcurrentHashMap<>(lastProcessTime));
        return statistics;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalDeadLetterCount.set(0);
        topicDeadLetterCount.clear();
        lastProcessTime.clear();
        logger.info("Dead letter statistics reset");
    }

    /**
     * 检查死信消息是否可以重新处理
     *
     * @param deadLetterMessage 死信消息
     * @return 是否可以重新处理
     */
    public boolean canReprocess(DefaultDeadLetterQueueHandler.DeadLetterMessage deadLetterMessage) {
        if (deadLetterMessage == null) {
            return false;
        }

        // 检查消息是否过期（例如：超过7天的死信消息不再处理）
        if (deadLetterMessage.getFailureTime() != null) {
            LocalDateTime expirationTime = deadLetterMessage.getFailureTime().plusDays(7);
            if (LocalDateTime.now().isAfter(expirationTime)) {
                logger.warn("Dead letter message expired, cannot reprocess: messageId={}, failureTime={}",
                        deadLetterMessage.getOriginalMessageId(), deadLetterMessage.getFailureTime());
                return false;
            }
        }

        // 检查原始数据是否完整
        if (deadLetterMessage.getOriginalData() == null || deadLetterMessage.getOriginalData().length == 0) {
            logger.warn("Dead letter message has no original data, cannot reprocess: messageId={}",
                    deadLetterMessage.getOriginalMessageId());
            return false;
        }

        // 检查原始主题是否存在
        if (deadLetterMessage.getOriginalTopic() == null || deadLetterMessage.getOriginalTopic().trim().isEmpty()) {
            logger.warn("Dead letter message has no original topic, cannot reprocess: messageId={}",
                    deadLetterMessage.getOriginalMessageId());
            return false;
        }

        return true;
    }

    /**
     * 记录死信消息
     *
     * @param originalTopic 原始主题
     * @param message       原始消息
     * @param exception     异常信息
     */
    public void recordDeadLetterMessage(String originalTopic, Message<?> message, Exception exception) {
        // 更新统计信息
        totalDeadLetterCount.incrementAndGet();
        topicDeadLetterCount.computeIfAbsent(originalTopic, k -> new AtomicLong(0)).incrementAndGet();
        lastProcessTime.put(originalTopic, LocalDateTime.now());

        logger.debug("Dead letter message recorded: topic={}, messageId={}, totalCount={}",
                originalTopic, message.getMessageId(), totalDeadLetterCount.get());
    }

    /**
     * 构建重新处理的消息
     */
    private ReprocessedMessage buildReprocessedMessage(DefaultDeadLetterQueueHandler.DeadLetterMessage deadLetterMessage) {
        ReprocessedMessage reprocessedMessage = new ReprocessedMessage();
        reprocessedMessage.setOriginalMessageId(deadLetterMessage.getOriginalMessageId());
        reprocessedMessage.setOriginalData(deadLetterMessage.getOriginalData());
        reprocessedMessage.setOriginalProperties(deadLetterMessage.getOriginalProperties());
        reprocessedMessage.setReprocessTime(LocalDateTime.now());
        reprocessedMessage.setOriginalFailureReason(deadLetterMessage.getFailureReason());
        reprocessedMessage.setOriginalFailureTime(deadLetterMessage.getFailureTime());
        return reprocessedMessage;
    }

    /**
     * 更新重新处理统计信息
     */
    private void updateReprocessStatistics(String topic) {
        lastProcessTime.put(topic + "_reprocess", LocalDateTime.now());
    }

    /**
     * 重新处理的消息包装器
     */
    public static class ReprocessedMessage {
        private String originalMessageId;
        private byte[] originalData;
        private java.util.Map<String, String> originalProperties;
        private LocalDateTime reprocessTime;
        private String originalFailureReason;
        private LocalDateTime originalFailureTime;

        // Getters and Setters
        public String getOriginalMessageId() {
            return originalMessageId;
        }

        public void setOriginalMessageId(String originalMessageId) {
            this.originalMessageId = originalMessageId;
        }

        public byte[] getOriginalData() {
            return originalData;
        }

        public void setOriginalData(byte[] originalData) {
            this.originalData = originalData;
        }

        public java.util.Map<String, String> getOriginalProperties() {
            return originalProperties;
        }

        public void setOriginalProperties(java.util.Map<String, String> originalProperties) {
            this.originalProperties = originalProperties;
        }

        public LocalDateTime getReprocessTime() {
            return reprocessTime;
        }

        public void setReprocessTime(LocalDateTime reprocessTime) {
            this.reprocessTime = reprocessTime;
        }

        public String getOriginalFailureReason() {
            return originalFailureReason;
        }

        public void setOriginalFailureReason(String originalFailureReason) {
            this.originalFailureReason = originalFailureReason;
        }

        public LocalDateTime getOriginalFailureTime() {
            return originalFailureTime;
        }

        public void setOriginalFailureTime(LocalDateTime originalFailureTime) {
            this.originalFailureTime = originalFailureTime;
        }
    }

    /**
     * 批量重新处理结果
     */
    public static class BatchReprocessResult {
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final java.util.List<String> failedMessageIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        private int totalCount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public void incrementFailureCount() {
            failureCount.incrementAndGet();
        }

        public void addFailedMessage(String messageId) {
            failedMessageIds.add(messageId);
        }

        public long getDurationMillis() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMillis();
            }
            return 0;
        }

        // Getters and Setters
        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public long getSuccessCount() {
            return successCount.get();
        }

        public long getFailureCount() {
            return failureCount.get();
        }

        public java.util.List<String> getFailedMessageIds() {
            return failedMessageIds;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
    }

    /**
     * 死信统计信息
     */
    public static class DeadLetterStatistics {
        private long totalDeadLetterCount;
        private java.util.Map<String, AtomicLong> topicDeadLetterCount;
        private java.util.Map<String, LocalDateTime> lastProcessTime;

        // Getters and Setters
        public long getTotalDeadLetterCount() {
            return totalDeadLetterCount;
        }

        public void setTotalDeadLetterCount(long totalDeadLetterCount) {
            this.totalDeadLetterCount = totalDeadLetterCount;
        }

        public java.util.Map<String, AtomicLong> getTopicDeadLetterCount() {
            return topicDeadLetterCount;
        }

        public void setTopicDeadLetterCount(java.util.Map<String, AtomicLong> topicDeadLetterCount) {
            this.topicDeadLetterCount = topicDeadLetterCount;
        }

        public java.util.Map<String, LocalDateTime> getLastProcessTime() {
            return lastProcessTime;
        }

        public void setLastProcessTime(java.util.Map<String, LocalDateTime> lastProcessTime) {
            this.lastProcessTime = lastProcessTime;
        }
    }
}