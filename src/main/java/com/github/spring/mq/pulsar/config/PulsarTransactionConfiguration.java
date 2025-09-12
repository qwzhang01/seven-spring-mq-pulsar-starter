package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.exception.PulsarTransactionException;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.apache.pulsar.client.api.transaction.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Pulsar 事务配置类
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarTransactionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(PulsarTransactionConfiguration.class);

    /**
     * Pulsar 事务管理器
     */
    @Bean
    @ConditionalOnMissingBean(name = "pulsarTransactionManager")
    public PlatformTransactionManager pulsarTransactionManager(PulsarClient pulsarClient,
                                                               PulsarProperties pulsarProperties) {
        return new PulsarTransactionManager(pulsarClient, pulsarProperties);
    }

    /**
     * Pulsar 事务模板
     */
    @Bean
    @ConditionalOnMissingBean(name = "pulsarTransactionTemplate")
    public TransactionTemplate pulsarTransactionTemplate(PlatformTransactionManager pulsarTransactionManager) {
        return new TransactionTemplate(pulsarTransactionManager);
    }

    /**
     * Pulsar 事务构建器工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public PulsarTransactionBuilderFactory pulsarTransactionBuilderFactory(PulsarClient pulsarClient,
                                                                           PulsarProperties pulsarProperties) {
        return new PulsarTransactionBuilderFactory(pulsarClient, pulsarProperties);
    }

    /**
     * Pulsar 事务管理器实现
     */
    public static class PulsarTransactionManager extends AbstractPlatformTransactionManager {

        private final PulsarClient pulsarClient;
        private final PulsarProperties pulsarProperties;

        public PulsarTransactionManager(PulsarClient pulsarClient, PulsarProperties pulsarProperties) {
            this.pulsarClient = pulsarClient;
            this.pulsarProperties = pulsarProperties;
            setTransactionSynchronization(SYNCHRONIZATION_ALWAYS);
        }

        @Override
        protected Object doGetTransaction() throws TransactionException {
            PulsarTransactionObject txObject = new PulsarTransactionObject();

            // 检查是否已存在事务
            PulsarTransactionHolder holder = (PulsarTransactionHolder)
                    TransactionSynchronizationManager.getResource(pulsarClient);

            if (holder != null) {
                txObject.setTransactionHolder(holder);
            }

            return txObject;
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) throws TransactionException {
            PulsarTransactionObject txObject = (PulsarTransactionObject) transaction;
            return txObject.hasTransaction();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
            PulsarTransactionObject txObject = (PulsarTransactionObject) transaction;

            try {
                TransactionBuilder txnBuilder = pulsarClient.newTransaction();

                // 设置事务超时时间
                long timeoutMs = pulsarProperties.getTransaction().getTimeout().toMillis();
                if (definition.getTimeout() != TransactionDefinition.TIMEOUT_DEFAULT) {
                    timeoutMs = definition.getTimeout() * 1000L;
                }
                txnBuilder.withTransactionTimeout(timeoutMs, TimeUnit.MILLISECONDS);

                // 创建事务
                CompletableFuture<Transaction> txnFuture = txnBuilder.build();
                Transaction transaction1 = txnFuture.get(timeoutMs, TimeUnit.MILLISECONDS);

                PulsarTransactionHolder holder = new PulsarTransactionHolder(transaction1);
                txObject.setTransactionHolder(holder);

                // 绑定事务到当前线程
                TransactionSynchronizationManager.bindResource(pulsarClient, holder);

                logger.debug("Started Pulsar transaction: " + transaction1.getTxnID());

            } catch (Exception e) {
                throw new PulsarTransactionException("Could not start Pulsar transaction", e);
            }
        }

        @Override
        protected Object doSuspend(Object transaction) throws TransactionException {
            PulsarTransactionObject txObject = (PulsarTransactionObject) transaction;
            txObject.setTransactionHolder(null);
            return TransactionSynchronizationManager.unbindResource(pulsarClient);
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) throws TransactionException {
            PulsarTransactionHolder holder = (PulsarTransactionHolder) suspendedResources;
            TransactionSynchronizationManager.bindResource(pulsarClient, holder);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
            PulsarTransactionObject txObject = (PulsarTransactionObject) status.getTransaction();
            Transaction transaction = txObject.getTransactionHolder().getTransaction();

            try {
                transaction.commit().get();
                logger.debug("Committed Pulsar transaction: " + transaction.getTxnID());
            } catch (Exception e) {
                throw new PulsarTransactionException("Could not commit Pulsar transaction", e);
            }
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
            PulsarTransactionObject txObject = (PulsarTransactionObject) status.getTransaction();
            Transaction transaction = txObject.getTransactionHolder().getTransaction();

            try {
                transaction.abort().get();
                logger.debug("Rolled back Pulsar transaction: " + transaction.getTxnID());
            } catch (Exception e) {
                throw new PulsarTransactionException("Could not rollback Pulsar transaction", e);
            }
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            PulsarTransactionObject txObject = (PulsarTransactionObject) transaction;

            // 解绑资源
            TransactionSynchronizationManager.unbindResource(pulsarClient);

            txObject.setTransactionHolder(null);
        }
    }

    /**
     * Pulsar 事务对象
     */
    public static class PulsarTransactionObject {
        private PulsarTransactionHolder transactionHolder;

        public PulsarTransactionHolder getTransactionHolder() {
            return transactionHolder;
        }

        public void setTransactionHolder(PulsarTransactionHolder transactionHolder) {
            this.transactionHolder = transactionHolder;
        }

        public boolean hasTransaction() {
            return transactionHolder != null && transactionHolder.getTransaction() != null;
        }
    }

    /**
     * Pulsar 事务持有者
     */
    public static class PulsarTransactionHolder {
        private final Transaction transaction;

        public PulsarTransactionHolder(Transaction transaction) {
            this.transaction = transaction;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }

    /**
     * Pulsar 事务构建器工厂
     */
    public static class PulsarTransactionBuilderFactory {
        private final PulsarClient pulsarClient;
        private final PulsarProperties pulsarProperties;

        public PulsarTransactionBuilderFactory(PulsarClient pulsarClient, PulsarProperties pulsarProperties) {
            this.pulsarClient = pulsarClient;
            this.pulsarProperties = pulsarProperties;
        }

        /**
         * 创建事务构建器
         */
        public TransactionBuilder newTransactionBuilder() {
            return pulsarClient.newTransaction()
                    .withTransactionTimeout(
                            pulsarProperties.getTransaction().getTimeout().toMillis(),
                            TimeUnit.MILLISECONDS
                    );
        }

        /**
         * 获取当前事务
         */
        public Transaction getCurrentTransaction() {
            PulsarTransactionHolder holder = (PulsarTransactionHolder)
                    TransactionSynchronizationManager.getResource(pulsarClient);
            return holder != null ? holder.getTransaction() : null;
        }
    }
}