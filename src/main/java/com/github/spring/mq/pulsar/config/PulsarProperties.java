/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.exception.PulsarConfigUnsupportedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Pulsar configuration properties
 *
 * <p>This class contains all configuration properties for Pulsar integration.
 * It supports configuration for producers, consumers, clients, transactions,
 * dead letter queues, and other Pulsar features.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.pulsar")
public class PulsarProperties {

    private boolean enabled = true;

    /**
     * Pulsar service URL
     */
    private String serviceUrl = "pulsar://localhost:6650";

    /**
     * Authentication configuration
     */
    private Authentication authentication = new Authentication();

    /**
     * Producer configuration
     */
    private Producer producer = new Producer();
    /**
     * Multiple producers configuration
     */
    @NestedConfigurationProperty
    private Map<String, Producer> producerMap = new HashMap<>();

    /**
     * Consumer configuration
     */
    private Consumer consumer = new Consumer();

    /**
     * Multiple consumers configuration
     */
    @NestedConfigurationProperty
    private Map<String, Consumer> consumerMap = new HashMap<>();

    /**
     * Client configuration
     */
    private Client client = new Client();

    /**
     * Transaction configuration
     */
    private Transaction transaction = new Transaction();

    private DeadLetterQueueProperties deadLetter;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public Map<String, Producer> getProducerMap() {
        return producerMap;
    }

    public void setProducerMap(Map<String, Producer> producerMap) {
        this.producerMap = producerMap;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Map<String, Consumer> getConsumerMap() {
        return consumerMap;
    }

    public void setConsumerMap(Map<String, Consumer> consumerMap) {
        this.consumerMap = consumerMap;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public DeadLetterQueueProperties getDeadLetter() {
        return deadLetter;
    }

    public void setDeadLetter(DeadLetterQueueProperties deadLetter) {
        this.deadLetter = deadLetter;
    }

    /**
     * Validate configuration parameters
     */
    public void valid() {
        validProduce();
        validConsume();
    }

    private void validConsume() {
        Consumer consumer = getConsumer();
        Map<String, Consumer> consumerMap = getConsumerMap();
        if (consumer != null && consumerMap != null) {
            if (StringUtils.isNotBlank(consumer.getTopic()) && !consumerMap.isEmpty()) {
                throw new PulsarConfigUnsupportedException("Single consumer and multiple consumers cannot be configured simultaneously, please configure only one before starting");
            }
        }
    }

    private void validProduce() {
        Producer producer = getProducer();
        Map<String, Producer> producerMap = getProducerMap();
        if (producer != null && producerMap != null) {
            if (StringUtils.isNotBlank(producer.getTopic()) && !producerMap.isEmpty()) {
                throw new PulsarConfigUnsupportedException("Single producer and multiple producers cannot be configured simultaneously, please configure only one before starting");
            }
        }
    }

    /**
     * Authentication configuration
     */
    public static class Authentication {
        private boolean enabled = true;
        private String token;
        private String authPluginClassName;
        private Map<String, String> authParams = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getAuthPluginClassName() {
            return authPluginClassName;
        }

        public void setAuthPluginClassName(String authPluginClassName) {
            this.authPluginClassName = authPluginClassName;
        }

        public Map<String, String> getAuthParams() {
            return authParams;
        }

        public void setAuthParams(Map<String, String> authParams) {
            this.authParams = authParams;
        }
    }

    /**
     * Producer configuration
     */
    public static class Producer {
        private String topic;
        private Duration sendTimeout = Duration.ofSeconds(30);
        private boolean blockIfQueueFull = false;
        private int maxPendingMessages = 1000;
        private boolean batchingEnabled = true;
        private int batchingMaxMessages = 1000;
        private Duration batchingMaxPublishDelay = Duration.ofMillis(10);

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public Duration getSendTimeout() {
            return sendTimeout;
        }

        public void setSendTimeout(Duration sendTimeout) {
            this.sendTimeout = sendTimeout;
        }

        public boolean isBlockIfQueueFull() {
            return blockIfQueueFull;
        }

        public void setBlockIfQueueFull(boolean blockIfQueueFull) {
            this.blockIfQueueFull = blockIfQueueFull;
        }

        public int getMaxPendingMessages() {
            return maxPendingMessages;
        }

        public void setMaxPendingMessages(int maxPendingMessages) {
            this.maxPendingMessages = maxPendingMessages;
        }

        public boolean isBatchingEnabled() {
            return batchingEnabled;
        }

        public void setBatchingEnabled(boolean batchingEnabled) {
            this.batchingEnabled = batchingEnabled;
        }

        public int getBatchingMaxMessages() {
            return batchingMaxMessages;
        }

        public void setBatchingMaxMessages(int batchingMaxMessages) {
            this.batchingMaxMessages = batchingMaxMessages;
        }

        public Duration getBatchingMaxPublishDelay() {
            return batchingMaxPublishDelay;
        }

        public void setBatchingMaxPublishDelay(Duration batchingMaxPublishDelay) {
            this.batchingMaxPublishDelay = batchingMaxPublishDelay;
        }
    }

    /**
     * Consumer configuration
     */
    public static class Consumer {

        private String topic;
        private String retryTopic;
        private String deadTopic;
        /**
         * Field used to distinguish different business types for the same message
         */
        private String businessKey = "businessPath";

        private String subscriptionName = "sub1";
        private String deadTopicSubscriptionName = "sub1";
        /**
         * Subscription type: Exclusive, Shared, Failover, Key_Shared
         */
        private String subscriptionType = "Shared";
        /**
         * Subscription initial position for new subscriptions: Earliest, Latest
         */
        private String subscriptionInitialPosition = "Earliest";
        private boolean autoAck = true;
        /**
         * Maximum retry count, messages enter dead letter queue after exceeding this limit
         */
        private int retryTime = 3;
        /**
         * Messages without Ack will be redelivered after 30 seconds by default
         */
        private Duration ackTimeout = Duration.ofSeconds(30);
        /**
         * Sets the size of the consumer receive queue
         */
        private int receiverQueueSize = 1000;
        /**
         * Redelivery delay time for negativeAck messages
         * Default is 1000 milliseconds
         */
        private int negativeAckRedeliveryDelay = 1000;
        /**
         * Message redelivery delay time
         */
        private int timeToReconsumeDelay = 1000;

        private boolean autoAckOldestChunkedMessageOnQueueFull = false;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public boolean isAutoAck() {
            return autoAck;
        }

        public void setAutoAck(boolean autoAck) {
            this.autoAck = autoAck;
        }

        public String getDeadTopicSubscriptionName() {
            return deadTopicSubscriptionName;
        }

        public void setDeadTopicSubscriptionName(String deadTopicSubscriptionName) {
            this.deadTopicSubscriptionName = deadTopicSubscriptionName;
        }

        public String getRetryTopic() {
            return retryTopic;
        }

        public void setRetryTopic(String retryTopic) {
            this.retryTopic = retryTopic;
        }

        public String getBusinessKey() {
            return businessKey;
        }

        public void setBusinessKey(String businessKey) {
            this.businessKey = businessKey;
        }

        public int getTimeToReconsumeDelay() {
            return timeToReconsumeDelay;
        }

        public void setTimeToReconsumeDelay(int timeToReconsumeDelay) {
            this.timeToReconsumeDelay = timeToReconsumeDelay;
        }

        public String getDeadTopic() {
            return deadTopic;
        }

        public void setDeadTopic(String deadTopic) {
            this.deadTopic = deadTopic;
        }

        public String getSubscriptionName() {
            return subscriptionName;
        }

        public void setSubscriptionName(String subscriptionName) {
            this.subscriptionName = subscriptionName;
        }

        public String getSubscriptionType() {
            return subscriptionType;
        }

        public void setSubscriptionType(String subscriptionType) {
            this.subscriptionType = subscriptionType;
        }

        public int getRetryTime() {
            return retryTime;
        }

        public void setRetryTime(int retryTime) {
            this.retryTime = retryTime;
        }

        public String getSubscriptionInitialPosition() {
            return subscriptionInitialPosition;
        }

        public void setSubscriptionInitialPosition(String subscriptionInitialPosition) {
            this.subscriptionInitialPosition = subscriptionInitialPosition;
        }

        public int getNegativeAckRedeliveryDelay() {
            return negativeAckRedeliveryDelay;
        }

        public void setNegativeAckRedeliveryDelay(int negativeAckRedeliveryDelay) {
            this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
        }

        public Duration getAckTimeout() {
            return ackTimeout;
        }

        public void setAckTimeout(Duration ackTimeout) {
            this.ackTimeout = ackTimeout;
        }

        public int getReceiverQueueSize() {
            return receiverQueueSize;
        }

        public void setReceiverQueueSize(int receiverQueueSize) {
            this.receiverQueueSize = receiverQueueSize;
        }

        public boolean isAutoAckOldestChunkedMessageOnQueueFull() {
            return autoAckOldestChunkedMessageOnQueueFull;
        }

        public void setAutoAckOldestChunkedMessageOnQueueFull(boolean autoAckOldestChunkedMessageOnQueueFull) {
            this.autoAckOldestChunkedMessageOnQueueFull = autoAckOldestChunkedMessageOnQueueFull;
        }
    }

    /**
     * Client configuration
     */
    public static class Client {
        private Duration operationTimeout = Duration.ofSeconds(30);
        private Duration connectionTimeout = Duration.ofSeconds(10);
        private int numIoThreads = 1;
        private int numListenerThreads = 1;

        public Duration getOperationTimeout() {
            return operationTimeout;
        }

        public void setOperationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
        }

        public Duration getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getNumIoThreads() {
            return numIoThreads;
        }

        public void setNumIoThreads(int numIoThreads) {
            this.numIoThreads = numIoThreads;
        }

        public int getNumListenerThreads() {
            return numListenerThreads;
        }

        public void setNumListenerThreads(int numListenerThreads) {
            this.numListenerThreads = numListenerThreads;
        }
    }

    /**
     * Transaction configuration
     */
    public static class Transaction {
        /**
         * Whether to enable transactions
         */
        private boolean enabled = false;

        /**
         * Transaction coordinator topic
         */
        private String coordinatorTopic = "persistent://pulsar/system/transaction_coordinator_assign";

        /**
         * Transaction timeout
         */
        private Duration timeout = Duration.ofMinutes(1);

        /**
         * Transaction buffer snapshot segment size
         */
        private int bufferSnapshotSegmentSize = 1024 * 1024; // 1MB

        /**
         * Transaction buffer snapshot minimum time interval
         */
        private Duration bufferSnapshotMinTimeInMillis = Duration.ofSeconds(5);

        /**
         * Transaction buffer snapshot maximum transaction count
         */
        private int bufferSnapshotMaxTransactionCount = 1000;

        /**
         * Transaction log store size
         */
        private long logStoreSize = 1024 * 1024 * 1024L; // 1GB

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCoordinatorTopic() {
            return coordinatorTopic;
        }

        public void setCoordinatorTopic(String coordinatorTopic) {
            this.coordinatorTopic = coordinatorTopic;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getBufferSnapshotSegmentSize() {
            return bufferSnapshotSegmentSize;
        }

        public void setBufferSnapshotSegmentSize(int bufferSnapshotSegmentSize) {
            this.bufferSnapshotSegmentSize = bufferSnapshotSegmentSize;
        }

        public Duration getBufferSnapshotMinTimeInMillis() {
            return bufferSnapshotMinTimeInMillis;
        }

        public void setBufferSnapshotMinTimeInMillis(Duration bufferSnapshotMinTimeInMillis) {
            this.bufferSnapshotMinTimeInMillis = bufferSnapshotMinTimeInMillis;
        }

        public int getBufferSnapshotMaxTransactionCount() {
            return bufferSnapshotMaxTransactionCount;
        }

        public void setBufferSnapshotMaxTransactionCount(int bufferSnapshotMaxTransactionCount) {
            this.bufferSnapshotMaxTransactionCount = bufferSnapshotMaxTransactionCount;
        }

        public long getLogStoreSize() {
            return logStoreSize;
        }

        public void setLogStoreSize(long logStoreSize) {
            this.logStoreSize = logStoreSize;
        }
    }


    /**
     * Dead letter queue configuration properties
     */
    public static class DeadLetterQueueProperties {

        /**
         * Dead letter queue topic suffix
         */
        private String topicSuffix = "-DLQ";

        /**
         * Maximum retry count
         */
        private int maxRetries = 3;

        /**
         * Retry configuration
         */
        private Retry retry = new Retry();

        /**
         * Cleanup configuration
         */
        private Cleanup cleanup = new Cleanup();

        /**
         * Monitoring configuration
         */
        private Monitoring monitoring = new Monitoring();

        /**
         * Statistics configuration
         */
        private Statistics statistics = new Statistics();

        public String getTopicSuffix() {
            return topicSuffix;
        }

        public void setTopicSuffix(String topicSuffix) {
            this.topicSuffix = topicSuffix;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public Cleanup getCleanup() {
            return cleanup;
        }

        public void setCleanup(Cleanup cleanup) {
            this.cleanup = cleanup;
        }

        public Monitoring getMonitoring() {
            return monitoring;
        }

        public void setMonitoring(Monitoring monitoring) {
            this.monitoring = monitoring;
        }

        public Statistics getStatistics() {
            return statistics;
        }

        public void setStatistics(Statistics statistics) {
            this.statistics = statistics;
        }

        /**
         * Retry configuration
         */
        public static class Retry {
            /**
             * Whether to enable smart retry strategy
             */
            private boolean smartStrategyEnabled = true;

            /**
             * Base retry delay
             */
            private Duration baseDelay = Duration.ofSeconds(1);

            /**
             * Maximum retry delay
             */
            private Duration maxDelay = Duration.ofMinutes(5);

            /**
             * Retry time window
             */
            private Duration retryWindow = Duration.ofHours(24);

            /**
             * Whether to enable jitter
             */
            private boolean jitterEnabled = true;

            /**
             * Jitter factor (0.0-1.0)
             */
            private double jitterFactor = 0.2;

            public boolean isSmartStrategyEnabled() {
                return smartStrategyEnabled;
            }

            public void setSmartStrategyEnabled(boolean smartStrategyEnabled) {
                this.smartStrategyEnabled = smartStrategyEnabled;
            }

            public Duration getBaseDelay() {
                return baseDelay;
            }

            public void setBaseDelay(Duration baseDelay) {
                this.baseDelay = baseDelay;
            }

            public Duration getMaxDelay() {
                return maxDelay;
            }

            public void setMaxDelay(Duration maxDelay) {
                this.maxDelay = maxDelay;
            }

            public Duration getRetryWindow() {
                return retryWindow;
            }

            public void setRetryWindow(Duration retryWindow) {
                this.retryWindow = retryWindow;
            }

            public boolean isJitterEnabled() {
                return jitterEnabled;
            }

            public void setJitterEnabled(boolean jitterEnabled) {
                this.jitterEnabled = jitterEnabled;
            }

            public double getJitterFactor() {
                return jitterFactor;
            }

            public void setJitterFactor(double jitterFactor) {
                this.jitterFactor = jitterFactor;
            }
        }

        /**
         * Cleanup configuration
         */
        public static class Cleanup {
            /**
             * Whether to enable automatic cleanup
             */
            private boolean autoCleanupEnabled = true;

            /**
             * Message expiration time
             */
            private Duration messageExpiration = Duration.ofDays(7);

            /**
             * Cleanup execution time (cron expression)
             */
            private String cleanupCron = "0 0 2 * * ?"; // Daily at 2 AM

            /**
             * Retry information expiration time
             */
            private Duration retryInfoExpiration = Duration.ofHours(24);

            public boolean isAutoCleanupEnabled() {
                return autoCleanupEnabled;
            }

            public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
                this.autoCleanupEnabled = autoCleanupEnabled;
            }

            public Duration getMessageExpiration() {
                return messageExpiration;
            }

            public void setMessageExpiration(Duration messageExpiration) {
                this.messageExpiration = messageExpiration;
            }

            public String getCleanupCron() {
                return cleanupCron;
            }

            public void setCleanupCron(String cleanupCron) {
                this.cleanupCron = cleanupCron;
            }

            public Duration getRetryInfoExpiration() {
                return retryInfoExpiration;
            }

            public void setRetryInfoExpiration(Duration retryInfoExpiration) {
                this.retryInfoExpiration = retryInfoExpiration;
            }
        }

        /**
         * Monitoring configuration
         */
        public static class Monitoring {
            /**
             * Whether to enable monitoring
             */
            private boolean enabled = true;

            /**
             * Monitoring interval
             */
            private Duration monitoringInterval = Duration.ofMinutes(5);

            /**
             * Health check timeout
             */
            private Duration healthCheckTimeout = Duration.ofSeconds(30);

            /**
             * Whether to enable alerts
             */
            private boolean alertEnabled = false;

            /**
             * Alert threshold (dead letter message count)
             */
            private long alertThreshold = 100;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Duration getMonitoringInterval() {
                return monitoringInterval;
            }

            public void setMonitoringInterval(Duration monitoringInterval) {
                this.monitoringInterval = monitoringInterval;
            }

            public Duration getHealthCheckTimeout() {
                return healthCheckTimeout;
            }

            public void setHealthCheckTimeout(Duration healthCheckTimeout) {
                this.healthCheckTimeout = healthCheckTimeout;
            }

            public boolean isAlertEnabled() {
                return alertEnabled;
            }

            public void setAlertEnabled(boolean alertEnabled) {
                this.alertEnabled = alertEnabled;
            }

            public long getAlertThreshold() {
                return alertThreshold;
            }

            public void setAlertThreshold(long alertThreshold) {
                this.alertThreshold = alertThreshold;
            }
        }

        /**
         * Statistics configuration
         */
        public static class Statistics {
            /**
             * Whether to enable statistics
             */
            private boolean enabled = true;

            /**
             * Statistics data retention period
             */
            private Duration retentionPeriod = Duration.ofDays(30);

            /**
             * Whether to enable detailed statistics
             */
            private boolean detailedEnabled = false;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Duration getRetentionPeriod() {
                return retentionPeriod;
            }

            public void setRetentionPeriod(Duration retentionPeriod) {
                this.retentionPeriod = retentionPeriod;
            }

            public boolean isDetailedEnabled() {
                return detailedEnabled;
            }

            public void setDetailedEnabled(boolean detailedEnabled) {
                this.detailedEnabled = detailedEnabled;
            }
        }
    }
}