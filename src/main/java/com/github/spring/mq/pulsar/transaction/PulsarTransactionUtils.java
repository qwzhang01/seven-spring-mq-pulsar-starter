package com.github.spring.mq.pulsar.transaction;

import com.github.spring.mq.pulsar.config.PulsarTransactionConfiguration;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Pulsar 事务工具类
 *
 * @author avinzhang
 * @since 1.0.0
 */
public class PulsarTransactionUtils {

    /**
     * 获取当前线程绑定的 Pulsar 事务
     *
     * @return 当前事务，如果没有事务则返回 null
     */
    public static Transaction getCurrentTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return null;
        }

        // 从事务同步管理器中获取事务资源
        Object resource = TransactionSynchronizationManager.getResourceMap().values()
                .stream()
                .filter(r -> r instanceof PulsarTransactionConfiguration.PulsarTransactionHolder)
                .findFirst()
                .orElse(null);

        if (resource instanceof PulsarTransactionConfiguration.PulsarTransactionHolder) {
            return ((PulsarTransactionConfiguration.PulsarTransactionHolder) resource).getTransaction();
        }

        return null;
    }

    /**
     * 检查当前是否存在 Pulsar 事务
     *
     * @return 如果存在事务返回 true，否则返回 false
     */
    public static boolean isTransactionActive() {
        return getCurrentTransaction() != null;
    }

    /**
     * 获取当前事务的 ID
     *
     * @return 事务 ID，如果没有事务则返回 null
     */
    public static String getCurrentTransactionId() {
        Transaction transaction = getCurrentTransaction();
        return transaction != null ? transaction.getTxnID().toString() : null;
    }
}