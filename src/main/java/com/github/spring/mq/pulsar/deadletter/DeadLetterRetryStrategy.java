package com.github.spring.mq.pulsar.deadletter;

import org.apache.pulsar.client.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死信队列重试策略
 * 提供智能的重试策略，包括指数退避、重试间隔控制等
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class DeadLetterRetryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterRetryStrategy.class);

    // 消息重试信息缓存
    private final ConcurrentHashMap<String, RetryInfo> retryInfoMap = new ConcurrentHashMap<>();

    /**
     * 计算重试延迟时间
     *
     * @param message    消息
     * @param retryCount 当前重试次数
     * @param exception  异常信息
     * @return 延迟时间（毫秒）
     */
    public long calculateRetryDelay(Message<?> message, int retryCount, Exception exception) {
        String messageId = message.getMessageId().toString();

        // 获取或创建重试信息
        RetryInfo retryInfo = retryInfoMap.computeIfAbsent(messageId, k -> new RetryInfo());
        retryInfo.setLastRetryTime(LocalDateTime.now());
        retryInfo.setRetryCount(retryCount);
        retryInfo.setLastException(exception.getClass().getSimpleName());

        // 根据异常类型确定重试策略
        RetryStrategyType strategyType = determineRetryStrategy(exception);

        long delayMs;
        switch (strategyType) {
            case EXPONENTIAL_BACKOFF:
                delayMs = calculateExponentialBackoff(retryCount);
                break;
            case FIXED_INTERVAL:
                delayMs = calculateFixedInterval();
                break;
            case LINEAR_BACKOFF:
                delayMs = calculateLinearBackoff(retryCount);
                break;
            case IMMEDIATE:
                delayMs = 0;
                break;
            default:
                delayMs = calculateExponentialBackoff(retryCount);
        }

        // 添加随机抖动避免雷群效应
        delayMs = addJitter(delayMs);

        retryInfo.setNextRetryDelay(delayMs);

        logger.debug("Calculated retry delay for message {}: {}ms (strategy: {}, retry: {})",
                messageId, delayMs, strategyType, retryCount);

        return delayMs;
    }

    /**
     * 判断是否应该重试
     *
     * @param message    消息
     * @param retryCount 重试次数
     * @param exception  异常信息
     * @return 是否应该重试
     */
    public boolean shouldRetry(Message<?> message, int retryCount, Exception exception) {
        // 检查重试次数限制
        if (retryCount >= getMaxRetries(exception)) {
            logger.warn("Max retry count reached for message {}: {}", message.getMessageId(), retryCount);
            return false;
        }

        // 检查是否是不可重试的异常
        if (isNonRetryableException(exception)) {
            logger.warn("Non-retryable exception for message {}: {}",
                    message.getMessageId(), exception.getClass().getSimpleName());
            return false;
        }

        // 检查重试时间窗口
        if (!isWithinRetryWindow(message)) {
            logger.warn("Message {} is outside retry window", message.getMessageId());
            return false;
        }

        return true;
    }

    /**
     * 清理过期的重试信息
     */
    public void cleanupExpiredRetryInfo() {
        LocalDateTime expirationTime = LocalDateTime.now().minusHours(24);

        retryInfoMap.entrySet().removeIf(entry -> {
            RetryInfo retryInfo = entry.getValue();
            return retryInfo.getLastRetryTime().isBefore(expirationTime);
        });

        logger.debug("Cleaned up expired retry info, remaining entries: {}", retryInfoMap.size());
    }

    /**
     * 获取重试信息
     *
     * @param messageId 消息ID
     * @return 重试信息
     */
    public RetryInfo getRetryInfo(String messageId) {
        return retryInfoMap.get(messageId);
    }

    /**
     * 确定重试策略类型
     */
    private RetryStrategyType determineRetryStrategy(Exception exception) {
        // 根据异常类型确定重试策略
        if (exception instanceof java.net.ConnectException ||
                exception instanceof java.net.SocketTimeoutException) {
            return RetryStrategyType.EXPONENTIAL_BACKOFF; // 网络异常使用指数退避
        } else if (exception instanceof IllegalArgumentException ||
                exception instanceof NullPointerException) {
            return RetryStrategyType.IMMEDIATE; // 参数异常立即重试一次
        } else if (exception instanceof java.util.concurrent.TimeoutException) {
            return RetryStrategyType.LINEAR_BACKOFF; // 超时异常使用线性退避
        } else {
            return RetryStrategyType.FIXED_INTERVAL; // 其他异常使用固定间隔
        }
    }

    /**
     * 计算指数退避延迟
     */
    private long calculateExponentialBackoff(int retryCount) {
        // 基础延迟：1秒
        long baseDelayMs = 1000;
        // 最大延迟：5分钟
        long maxDelayMs = 5 * 60 * 1000;

        long delayMs = baseDelayMs * (long) Math.pow(2, retryCount - 1);
        return Math.min(delayMs, maxDelayMs);
    }

    /**
     * 计算固定间隔延迟
     */
    private long calculateFixedInterval() {
        return 30 * 1000; // 30秒固定间隔
    }

    /**
     * 计算线性退避延迟
     */
    private long calculateLinearBackoff(int retryCount) {
        // 线性增长：每次重试增加10秒
        long baseDelayMs = 10 * 1000;
        long maxDelayMs = 2 * 60 * 1000; // 最大2分钟

        long delayMs = baseDelayMs * retryCount;
        return Math.min(delayMs, maxDelayMs);
    }

    /**
     * 添加随机抖动
     */
    private long addJitter(long delayMs) {
        // 添加±20%的随机抖动
        double jitterFactor = 0.8 + (Math.random() * 0.4); // 0.8 到 1.2
        return (long) (delayMs * jitterFactor);
    }

    /**
     * 获取最大重试次数
     */
    private int getMaxRetries(Exception exception) {
        // 根据异常类型确定最大重试次数
        if (exception instanceof java.net.ConnectException) {
            return 5; // 网络连接异常多重试几次
        } else if (exception instanceof IllegalArgumentException) {
            return 1; // 参数异常只重试一次
        } else {
            return 3; // 默认重试3次
        }
    }

    /**
     * 判断是否是不可重试的异常
     */
    private boolean isNonRetryableException(Exception exception) {
        return exception instanceof SecurityException ||
                exception instanceof UnsupportedOperationException ||
                exception instanceof ClassCastException;
    }

    /**
     * 检查是否在重试时间窗口内
     */
    private boolean isWithinRetryWindow(Message<?> message) {
        // 检查消息是否在24小时的重试窗口内
        long publishTime = message.getPublishTime();
        long currentTime = System.currentTimeMillis();
        long windowMs = 24 * 60 * 60 * 1000; // 24小时

        return (currentTime - publishTime) <= windowMs;
    }

    /**
     * 重试策略类型
     */
    public enum RetryStrategyType {
        EXPONENTIAL_BACKOFF,  // 指数退避
        FIXED_INTERVAL,       // 固定间隔
        LINEAR_BACKOFF,       // 线性退避
        IMMEDIATE            // 立即重试
    }

    /**
     * 重试信息
     */
    public static class RetryInfo {
        private LocalDateTime lastRetryTime;
        private int retryCount;
        private String lastException;
        private long nextRetryDelay;
        private RetryStrategyType strategyType;

        // Getters and Setters
        public LocalDateTime getLastRetryTime() {
            return lastRetryTime;
        }

        public void setLastRetryTime(LocalDateTime lastRetryTime) {
            this.lastRetryTime = lastRetryTime;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public String getLastException() {
            return lastException;
        }

        public void setLastException(String lastException) {
            this.lastException = lastException;
        }

        public long getNextRetryDelay() {
            return nextRetryDelay;
        }

        public void setNextRetryDelay(long nextRetryDelay) {
            this.nextRetryDelay = nextRetryDelay;
        }

        public RetryStrategyType getStrategyType() {
            return strategyType;
        }

        public void setStrategyType(RetryStrategyType strategyType) {
            this.strategyType = strategyType;
        }

        @Override
        public String toString() {
            return "RetryInfo{" +
                    "lastRetryTime=" + lastRetryTime +
                    ", retryCount=" + retryCount +
                    ", lastException='" + lastException + '\'' +
                    ", nextRetryDelay=" + nextRetryDelay +
                    ", strategyType=" + strategyType +
                    '}';
        }
    }
}