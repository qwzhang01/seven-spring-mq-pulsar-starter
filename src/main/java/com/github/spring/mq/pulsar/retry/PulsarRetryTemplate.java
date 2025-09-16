package com.github.spring.mq.pulsar.retry;

import com.github.spring.mq.pulsar.exception.PulsarRetryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Pulsar 重试模板
 * 提供消息处理失败时的重试机制
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarRetryTemplate {

    private static final Logger logger = LoggerFactory.getLogger(PulsarRetryTemplate.class);

    private final int maxRetries;
    private final long initialDelay;
    private final double multiplier;
    private final long maxDelay;
    private final boolean useRandomDelay;

    public PulsarRetryTemplate(int maxRetries, long initialDelay, double multiplier, long maxDelay, boolean useRandomDelay) {
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.multiplier = multiplier;
        this.maxDelay = maxDelay;
        this.useRandomDelay = useRandomDelay;
    }

    /**
     * 执行重试逻辑
     *
     * @param task 要执行的任务
     * @param <T>  返回类型
     * @return 执行结果
     * @throws Exception 最终执行失败时抛出异常
     */
    public <T> T execute(RetryableTask<T> task) throws Exception {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return task.execute();
            } catch (Exception e) {
                lastException = e;

                if (attempt == maxRetries) {
                    logger.error("Task failed after {} attempts", maxRetries + 1, e);
                    break;
                }

                long delay = calculateDelay(attempt);
                logger.warn("Task failed on attempt {}, retrying in {}ms", attempt + 1, delay, e);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PulsarRetryException("Retry interrupted", ie);
                }
            }
        }

        throw lastException;
    }

    /**
     * 计算延迟时间
     *
     * @param attempt 当前尝试次数
     * @return 延迟毫秒数
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (initialDelay * Math.pow(multiplier, attempt));
        delay = Math.min(delay, maxDelay);

        if (useRandomDelay) {
            // 添加随机抖动，避免雪崩效应
            delay = (long) (delay * (0.5 + ThreadLocalRandom.current().nextDouble() * 0.5));
        }

        return delay;
    }

    /**
     * 可重试任务接口
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    public interface RetryableTask<T> {
        T execute() throws Exception;
    }
}