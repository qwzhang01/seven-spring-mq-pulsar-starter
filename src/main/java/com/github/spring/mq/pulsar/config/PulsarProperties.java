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

    /**
     * 校验配置参数是否合法
     */
    public void valid() {
        validProduce();
    }

    private void validProduce() {
        Producer producer = getProducer();
        Map<String, Producer> producerMap = getProducerMap();
        if (producer != null && producerMap != null) {
            if (StringUtils.isNotBlank(producer.getTopic()) && !producerMap.isEmpty()) {
                throw new PulsarConfigUnsupportedException("但生产者与多生产者无法同时配置，请只配置其中一个后再启动");
            }
        }
    }

    /**
     * 认证配置
     */
    public static class Authentication {
        private boolean enabled = false;
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

        private String subscriptionName = "default-subscription";
        private String subscriptionType = "Exclusive";
        private Duration ackTimeout = Duration.ofSeconds(30);
        private int receiverQueueSize = 1000;
        private boolean autoAckOldestChunkedMessageOnQueueFull = false;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getRetryTopic() {
            return retryTopic;
        }

        public void setRetryTopic(String retryTopic) {
            this.retryTopic = retryTopic;
        }

        public void setDeadTopic(String deadTopic) {
            this.deadTopic = deadTopic;
        }

        public String getDeadTopic() {
            return deadTopic;
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
}