package com.github.spring.mq.pulsar.deadletter;

import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认死信队列处理器实现
 * 提供完整的死信队列处理功能，包括消息发送、统计、重试策略等
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DefaultDeadLetterQueueHandler implements DeadLetterQueueHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDeadLetterQueueHandler.class);

    private final PulsarTemplate pulsarTemplate;
    private final DeadLetterMessageProcessor deadLetterMessageProcessor;
    // 重试计数器 - 跟踪每个消息的重试次数
    private final ConcurrentHashMap<String, AtomicLong> retryCounters = new ConcurrentHashMap<>();
    // 死信统计
    private final AtomicLong totalDeadLetterCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> topicDeadLetterCount = new ConcurrentHashMap<>();

    public DefaultDeadLetterQueueHandler(PulsarTemplate pulsarTemplate, DeadLetterMessageProcessor deadLetterMessageProcessor) {
        this.pulsarTemplate = pulsarTemplate;
        this.deadLetterMessageProcessor = deadLetterMessageProcessor;
    }

    @Override
    public void handleDeadLetter(String originalTopic, Message<?> message, Exception exception) {
        String deadLetterTopic = getDeadLetterTopic(originalTopic);
        String messageId = message.getMessageId().toString();

        logger.error("Message sent to dead letter queue. Original topic: {}, Dead letter topic: {}, Message ID: {}",
                originalTopic, deadLetterTopic, messageId, exception);

        try {
            // 构建死信消息包装器
            DeadLetterMessage deadLetterMessage = buildDeadLetterMessage(originalTopic, message, exception);

            if (pulsarTemplate != null) {
                // 发送到死信队列
                pulsarTemplate.send(deadLetterTopic, deadLetterMessage);
                logger.info("Successfully sent message to dead letter queue: {}", deadLetterTopic);

                // 更新统计信息
                updateStatistics(originalTopic);

                // 记录到处理器（如果可用）
                if (deadLetterMessageProcessor != null) {
                    deadLetterMessageProcessor.recordDeadLetterMessage(originalTopic, message, exception);
                }

            } else {
                logger.warn("PulsarTemplate is not available, cannot send message to dead letter queue");
            }

            // 清理重试计数器
            cleanupRetryCounter(messageId);

        } catch (PulsarClientException e) {
            logger.error("Failed to send message to dead letter queue: {}", deadLetterTopic, e);

            // 如果发送死信消息失败，记录到本地日志或其他持久化存储
            logFailedDeadLetterMessage(originalTopic, message, exception, e);
        }
    }

    @Override
    public boolean shouldSendToDeadLetter(Message<?> message, Exception exception, int retryCount) {
        String messageId = message.getMessageId().toString();

        // 更新重试计数器
        AtomicLong counter = retryCounters.computeIfAbsent(messageId, k -> new AtomicLong(0));
        long currentRetryCount = counter.incrementAndGet();

        // 检查是否是不可重试的异常
        if (isNonRetryableException(exception)) {
            logger.warn("Non-retryable exception detected for message {}, sending to dead letter queue immediately: {}",
                    messageId, exception.getClass().getSimpleName());
            return true;
        }

        // 检查是否是致命异常（系统级错误）
        if (isFatalException(exception)) {
            logger.error("Fatal exception detected for message {}, sending to dead letter queue immediately: {}",
                    messageId, exception.getClass().getSimpleName());
            return true;
        }

        // 检查重试次数
        int maxRetries = getMaxRetries();
        boolean shouldSend = currentRetryCount >= maxRetries;

        if (shouldSend) {
            logger.warn("Max retry count ({}) reached for message {} (actual retries: {}), sending to dead letter queue",
                    maxRetries, messageId, currentRetryCount);
        } else {
            logger.debug("Message {} will be retried, current retry count: {}/{}",
                    messageId, currentRetryCount, maxRetries);
        }

        return shouldSend;
    }

    @Override
    public int getMaxRetries() {
        // 可以从配置中读取，这里先使用默认值
        return 3;
    }

    /**
     * 获取死信队列主题名称
     * 支持自定义命名策略
     */
    @Override
    public String getDeadLetterTopic(String originalTopic) {
        // 可以根据不同的主题使用不同的死信队列策略
        if (originalTopic.contains("private-message")) {
            return originalTopic + "-dlq";
        } else if (originalTopic.contains("system")) {
            return "system-dlq"; // 系统消息使用统一的死信队列
        } else {
            return originalTopic + "-dlq"; // 默认策略
        }
    }

    /**
     * 构建死信消息包装器
     *
     * @param originalTopic 原始主题
     * @param message       原始消息
     * @param exception     异常信息
     * @return 死信消息包装器
     */
    private DeadLetterMessage buildDeadLetterMessage(String originalTopic, Message<?> message, Exception exception) {
        DeadLetterMessage deadLetterMessage = new DeadLetterMessage();
        deadLetterMessage.setOriginalTopic(originalTopic);
        deadLetterMessage.setOriginalMessageId(message.getMessageId().toString());
        deadLetterMessage.setOriginalData(message.getData());
        deadLetterMessage.setFailureReason(exception.getMessage());
        deadLetterMessage.setExceptionClass(exception.getClass().getName());
        deadLetterMessage.setFailureTime(LocalDateTime.now());

        // 添加异常堆栈信息（截取前1000个字符避免过大）
        String stackTrace = getStackTrace(exception);
        if (stackTrace.length() > 1000) {
            stackTrace = stackTrace.substring(0, 1000) + "...";
        }
        deadLetterMessage.setStackTrace(stackTrace);

        // 添加原始消息的属性
        Map<String, String> originalProperties = new HashMap<>();
        if (message.getProperties() != null) {
            originalProperties.putAll(message.getProperties());
        }
        deadLetterMessage.setOriginalProperties(originalProperties);

        // 添加消息的其他元数据
        deadLetterMessage.setPublishTime(message.getPublishTime());
        deadLetterMessage.setEventTime(message.getEventTime());
        if (message.hasKey()) {
            deadLetterMessage.setOriginalKey(message.getKey());
        }

        // 添加重试次数信息
        String messageId = message.getMessageId().toString();
        AtomicLong retryCounter = retryCounters.get(messageId);
        if (retryCounter != null) {
            deadLetterMessage.setRetryCount(retryCounter.get());
        }

        return deadLetterMessage;
    }

    /**
     * 判断是否是不可重试的异常
     *
     * @param exception 异常
     * @return 是否不可重试
     */
    private boolean isNonRetryableException(Exception exception) {
        // 定义不可重试的异常类型
        return exception instanceof IllegalArgumentException ||
                exception instanceof SecurityException ||
                exception instanceof UnsupportedOperationException ||
                exception instanceof ClassCastException ||
                exception instanceof NullPointerException ||
                exception instanceof NumberFormatException ||
                exception instanceof IllegalStateException;
    }

    /**
     * 判断是否是致命异常
     *
     * @param exception 异常
     * @return 是否是致命异常
     */
    private boolean isFatalException(Exception exception) {
        // 定义致命异常类型（系统级错误）
        return exception.getCause() instanceof OutOfMemoryError;
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics(String originalTopic) {
        totalDeadLetterCount.incrementAndGet();
        topicDeadLetterCount.computeIfAbsent(originalTopic, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 清理重试计数器
     */
    private void cleanupRetryCounter(String messageId) {
        retryCounters.remove(messageId);
    }

    /**
     * 记录发送死信消息失败的情况
     */
    private void logFailedDeadLetterMessage(String originalTopic, Message<?> message,
                                            Exception originalException, Exception sendException) {
        logger.error("CRITICAL: Failed to send message to dead letter queue. " +
                        "Original topic: {}, Message ID: {}, Original exception: {}, Send exception: {}",
                originalTopic, message.getMessageId(), originalException.getMessage(), sendException.getMessage());

        // 这里可以添加其他的持久化逻辑，比如：
        // 1. 写入本地文件
        // 2. 发送到监控系统
        // 3. 存储到数据库
        // 4. 发送告警通知
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception exception) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 获取死信统计信息
     */
    public DeadLetterStatistics getStatistics() {
        DeadLetterStatistics statistics = new DeadLetterStatistics();
        statistics.setTotalDeadLetterCount(totalDeadLetterCount.get());
        statistics.setTopicDeadLetterCount(new HashMap<>());

        // 转换AtomicLong到Long
        for (Map.Entry<String, AtomicLong> entry : topicDeadLetterCount.entrySet()) {
            statistics.getTopicDeadLetterCount().put(entry.getKey(), entry.getValue().get());
        }

        statistics.setActiveRetryCount(retryCounters.size());
        return statistics;
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalDeadLetterCount.set(0);
        topicDeadLetterCount.clear();
        retryCounters.clear();
        logger.info("Dead letter handler statistics reset");
    }

    /**
     * 死信消息包装器
     */
    public static class DeadLetterMessage {
        private String originalTopic;
        private String originalMessageId;
        private byte[] originalData;
        private String originalKey;
        private Map<String, String> originalProperties;
        private String failureReason;
        private String exceptionClass;
        private String stackTrace;
        private LocalDateTime failureTime;
        private long publishTime;
        private long eventTime;
        private long retryCount;

        // Getters and Setters
        public String getOriginalTopic() {
            return originalTopic;
        }

        public void setOriginalTopic(String originalTopic) {
            this.originalTopic = originalTopic;
        }

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

        public String getOriginalKey() {
            return originalKey;
        }

        public void setOriginalKey(String originalKey) {
            this.originalKey = originalKey;
        }

        public Map<String, String> getOriginalProperties() {
            return originalProperties;
        }

        public void setOriginalProperties(Map<String, String> originalProperties) {
            this.originalProperties = originalProperties;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public String getExceptionClass() {
            return exceptionClass;
        }

        public void setExceptionClass(String exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }

        public LocalDateTime getFailureTime() {
            return failureTime;
        }

        public void setFailureTime(LocalDateTime failureTime) {
            this.failureTime = failureTime;
        }

        public long getPublishTime() {
            return publishTime;
        }

        public void setPublishTime(long publishTime) {
            this.publishTime = publishTime;
        }

        public long getEventTime() {
            return eventTime;
        }

        public void setEventTime(long eventTime) {
            this.eventTime = eventTime;
        }

        public long getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(long retryCount) {
            this.retryCount = retryCount;
        }

        @Override
        public String toString() {
            return "DeadLetterMessage{" +
                    "originalTopic='" + originalTopic + '\'' +
                    ", originalMessageId='" + originalMessageId + '\'' +
                    ", failureReason='" + failureReason + '\'' +
                    ", exceptionClass='" + exceptionClass + '\'' +
                    ", failureTime=" + failureTime +
                    ", retryCount=" + retryCount +
                    '}';
        }
    }

    /**
     * 死信统计信息
     */
    public static class DeadLetterStatistics {
        private long totalDeadLetterCount;
        private Map<String, Long> topicDeadLetterCount;
        private int activeRetryCount;

        // Getters and Setters
        public long getTotalDeadLetterCount() {
            return totalDeadLetterCount;
        }

        public void setTotalDeadLetterCount(long totalDeadLetterCount) {
            this.totalDeadLetterCount = totalDeadLetterCount;
        }

        public Map<String, Long> getTopicDeadLetterCount() {
            return topicDeadLetterCount;
        }

        public void setTopicDeadLetterCount(Map<String, Long> topicDeadLetterCount) {
            this.topicDeadLetterCount = topicDeadLetterCount;
        }

        public int getActiveRetryCount() {
            return activeRetryCount;
        }

        public void setActiveRetryCount(int activeRetryCount) {
            this.activeRetryCount = activeRetryCount;
        }

        @Override
        public String toString() {
            return "DeadLetterStatistics{" +
                    "totalDeadLetterCount=" + totalDeadLetterCount +
                    ", topicDeadLetterCount=" + topicDeadLetterCount +
                    ", activeRetryCount=" + activeRetryCount +
                    '}';
        }
    }
}