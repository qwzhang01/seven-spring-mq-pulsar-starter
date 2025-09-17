package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.exception.PulsarConfigUnsupportedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Pulsar 配置属性
 */
@ConfigurationProperties(prefix = "spring.pulsar")
public class PulsarProperties {

    /**
     * Pulsar 服务地址
     */
    private String serviceUrl = "pulsar://localhost:6650";

    /**
     * 认证配置
     */
    private Authentication authentication = new Authentication();

    /**
     * 生产者配置
     */
    private Producer producer = new Producer();
    /**
     * 多个生产者
     */
    @NestedConfigurationProperty
    private Map<String, Producer> producerMap = new HashMap<>();

    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 多个消费者
     */
    @NestedConfigurationProperty
    private Map<String, Consumer> consumerMap = new HashMap<>();

    /**
     * 客户端配置
     */
    private Client client = new Client();

    /**
     * 事务配置
     */
    private Transaction transaction = new Transaction();

    private DeadLetterQueueProperties deadLetter;

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
     * 校验配置参数是否合法
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
                throw new PulsarConfigUnsupportedException("单消费者与多消费者无法同时配置，请只配置其中一个后再启动");
            }
        }
    }

    private void validProduce() {
        Producer producer = getProducer();
        Map<String, Producer> producerMap = getProducerMap();
        if (producer != null && producerMap != null) {
            if (StringUtils.isNotBlank(producer.getTopic()) && !producerMap.isEmpty()) {
                throw new PulsarConfigUnsupportedException("单生产者与多生产者无法同时配置，请只配置其中一个后再启动");
            }
        }
    }

    /**
     * 认证配置
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
     * 生产者配置
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
     * 消费者配置
     */
    public static class Consumer {

        private String topic;
        private String retryTopic;
        private String deadTopic;
        /**
         * 同一个消息，不同业务区分使用的字段
         */
        private String businessKey = "businessPath";

        private String subscriptionName = "sub1";
        /**
         * 订阅模式 Exclusive,Shared,Failover,Key_Shared
         */
        private String subscriptionType = "Shared";
        /**
         * 创建新订阅的消费模式，Earliest, Latest
         */
        private String subscriptionInitialPosition = "Earliest";
        private boolean autoAck = true;
        private int retryTime = 3;
        /**
         * 没有Ack的消息，默认30秒后重新消费
         */
        private Duration ackTimeout = Duration.ofSeconds(30);
        /**
         * Sets the size of the consumer receive queue.
         */
        private int receiverQueueSize = 1000;
        /**
         * negativeAck的消息，重新消费延迟时间
         * 默认1000毫秒
         */
        private int negativeAckRedeliveryDelay = 1000;
        /**
         * 消息重新消费延迟时间
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
     * 客户端配置
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
     * 事务配置
     */
    public static class Transaction {
        /**
         * 是否启用事务
         */
        private boolean enabled = false;

        /**
         * 事务协调器主题
         */
        private String coordinatorTopic = "persistent://pulsar/system/transaction_coordinator_assign";

        /**
         * 事务超时时间
         */
        private Duration timeout = Duration.ofMinutes(1);

        /**
         * 事务缓冲区快照段大小
         */
        private int bufferSnapshotSegmentSize = 1024 * 1024; // 1MB

        /**
         * 事务缓冲区快照最小时间间隔
         */
        private Duration bufferSnapshotMinTimeInMillis = Duration.ofSeconds(5);

        /**
         * 事务缓冲区快照最大事务数
         */
        private int bufferSnapshotMaxTransactionCount = 1000;

        /**
         * 事务日志存储大小
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
     * 死信队列配置属性
     */
    public static class DeadLetterQueueProperties {

        /**
         * 死信队列主题后缀
         */
        private String topicSuffix = "-DLQ";

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 重试配置
         */
        private Retry retry = new Retry();

        /**
         * 清理配置
         */
        private Cleanup cleanup = new Cleanup();

        /**
         * 监控配置
         */
        private Monitoring monitoring = new Monitoring();

        /**
         * 统计配置
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
         * 重试配置
         */
        public static class Retry {
            /**
             * 是否启用智能重试策略
             */
            private boolean smartStrategyEnabled = true;

            /**
             * 基础重试延迟
             */
            private Duration baseDelay = Duration.ofSeconds(1);

            /**
             * 最大重试延迟
             */
            private Duration maxDelay = Duration.ofMinutes(5);

            /**
             * 重试时间窗口
             */
            private Duration retryWindow = Duration.ofHours(24);

            /**
             * 是否启用抖动
             */
            private boolean jitterEnabled = true;

            /**
             * 抖动因子（0.0-1.0）
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
         * 清理配置
         */
        public static class Cleanup {
            /**
             * 是否启用自动清理
             */
            private boolean autoCleanupEnabled = true;

            /**
             * 消息过期时间
             */
            private Duration messageExpiration = Duration.ofDays(7);

            /**
             * 清理执行时间（cron表达式）
             */
            private String cleanupCron = "0 0 2 * * ?"; // 每天凌晨2点

            /**
             * 重试信息过期时间
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
         * 监控配置
         */
        public static class Monitoring {
            /**
             * 是否启用监控
             */
            private boolean enabled = true;

            /**
             * 监控间隔
             */
            private Duration monitoringInterval = Duration.ofMinutes(5);

            /**
             * 健康检查超时时间
             */
            private Duration healthCheckTimeout = Duration.ofSeconds(30);

            /**
             * 是否启用告警
             */
            private boolean alertEnabled = false;

            /**
             * 告警阈值（死信消息数量）
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
         * 统计配置
         */
        public static class Statistics {
            /**
             * 是否启用统计
             */
            private boolean enabled = true;

            /**
             * 统计数据保留时间
             */
            private Duration retentionPeriod = Duration.ofDays(30);

            /**
             * 是否启用详细统计
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