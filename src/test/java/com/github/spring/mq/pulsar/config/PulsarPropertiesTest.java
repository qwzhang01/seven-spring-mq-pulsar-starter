package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.exception.PulsarConfigUnsupportedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PulsarProperties configuration validation
 *
 * @author avinzhang
 * @since 1.0.0
 */
@DisplayName("Pulsar Properties Tests")
class PulsarPropertiesTest {

    private PulsarProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PulsarProperties();
    }

    @Test
    @DisplayName("Should have default values")
    void shouldHaveDefaultValues() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getServiceUrl()).isEqualTo("pulsar://localhost:6650");
        assertThat(properties.getProducer()).isNotNull();
        assertThat(properties.getConsumer()).isNotNull();
        assertThat(properties.getClient()).isNotNull();
        assertThat(properties.getTransaction()).isNotNull();
    }

    @Test
    @DisplayName("Should validate producer configuration correctly")
    void shouldValidateProducerConfiguration() {
        // Valid single producer configuration
        properties.getProducer().setTopic("test-topic");
        assertThatCode(() -> properties.valid()).doesNotThrowAnyException();

        // Valid multiple producers configuration
        properties.getProducer().setTopic(null);
        Map<String, PulsarProperties.Producer> producerMap = new HashMap<>();
        PulsarProperties.Producer producer1 = new PulsarProperties.Producer();
        producer1.setTopic("topic1");
        producerMap.put("producer1", producer1);
        properties.setProducerMap(producerMap);

        assertThatCode(() -> properties.valid()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for conflicting producer configuration")
    void shouldThrowExceptionForConflictingProducerConfiguration() {
        // Both single and multiple producers configured
        properties.getProducer().setTopic("test-topic");
        Map<String, PulsarProperties.Producer> producerMap = new HashMap<>();
        PulsarProperties.Producer producer1 = new PulsarProperties.Producer();
        producer1.setTopic("topic1");
        producerMap.put("producer1", producer1);
        properties.setProducerMap(producerMap);

        assertThatThrownBy(() -> properties.valid())
                .isInstanceOf(PulsarConfigUnsupportedException.class)
                .hasMessageContaining("Single producer and multiple producers cannot be configured simultaneously");
    }

    @Test
    @DisplayName("Should validate consumer configuration correctly")
    void shouldValidateConsumerConfiguration() {
        // Valid single consumer configuration
        properties.getConsumer().setTopic("test-topic");
        assertThatCode(() -> properties.valid()).doesNotThrowAnyException();

        // Valid multiple consumers configuration
        properties.getConsumer().setTopic(null);
        Map<String, PulsarProperties.Consumer> consumerMap = new HashMap<>();
        PulsarProperties.Consumer consumer1 = new PulsarProperties.Consumer();
        consumer1.setTopic("topic1");
        consumerMap.put("consumer1", consumer1);
        properties.setConsumerMap(consumerMap);

        assertThatCode(() -> properties.valid()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for conflicting consumer configuration")
    void shouldThrowExceptionForConflictingConsumerConfiguration() {
        // Both single and multiple consumers configured
        properties.getConsumer().setTopic("test-topic");
        Map<String, PulsarProperties.Consumer> consumerMap = new HashMap<>();
        PulsarProperties.Consumer consumer1 = new PulsarProperties.Consumer();
        consumer1.setTopic("topic1");
        consumerMap.put("consumer1", consumer1);
        properties.setConsumerMap(consumerMap);

        assertThatThrownBy(() -> properties.valid())
                .isInstanceOf(PulsarConfigUnsupportedException.class)
                .hasMessageContaining("Single consumer and multiple consumers cannot be configured simultaneously");
    }

    @Test
    @DisplayName("Should configure producer properties correctly")
    void shouldConfigureProducerProperties() {
        PulsarProperties.Producer producer = properties.getProducer();

        producer.setTopic("test-topic");
        producer.setSendTimeout(Duration.ofSeconds(60));
        producer.setBlockIfQueueFull(true);
        producer.setMaxPendingMessages(2000);
        producer.setBatchingEnabled(false);
        producer.setBatchingMaxMessages(500);
        producer.setBatchingMaxPublishDelay(Duration.ofMillis(20));

        assertThat(producer.getTopic()).isEqualTo("test-topic");
        assertThat(producer.getSendTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(producer.isBlockIfQueueFull()).isTrue();
        assertThat(producer.getMaxPendingMessages()).isEqualTo(2000);
        assertThat(producer.isBatchingEnabled()).isFalse();
        assertThat(producer.getBatchingMaxMessages()).isEqualTo(500);
        assertThat(producer.getBatchingMaxPublishDelay()).isEqualTo(Duration.ofMillis(20));
    }

    @Test
    @DisplayName("Should configure consumer properties correctly")
    void shouldConfigureConsumerProperties() {
        PulsarProperties.Consumer consumer = properties.getConsumer();

        consumer.setTopic("test-topic");
        consumer.setRetryTopic("test-retry-topic");
        consumer.setDeadTopic("test-dead-topic");
        consumer.setSubscriptionName("test-subscription");
        consumer.setSubscriptionType("Exclusive");
        consumer.setSubscriptionInitialPosition("Latest");
        consumer.setAutoAck(false);
        consumer.setRetryTime(5);
        consumer.setAckTimeout(Duration.ofSeconds(60));
        consumer.setReceiverQueueSize(2000);
        consumer.setNegativeAckRedeliveryDelay(2000);

        assertThat(consumer.getTopic()).isEqualTo("test-topic");
        assertThat(consumer.getRetryTopic()).isEqualTo("test-retry-topic");
        assertThat(consumer.getDeadTopic()).isEqualTo("test-dead-topic");
        assertThat(consumer.getSubscriptionName()).isEqualTo("test-subscription");
        assertThat(consumer.getSubscriptionType()).isEqualTo("Exclusive");
        assertThat(consumer.getSubscriptionInitialPosition()).isEqualTo("Latest");
        assertThat(consumer.isAutoAck()).isFalse();
        assertThat(consumer.getRetryTime()).isEqualTo(5);
        assertThat(consumer.getAckTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(consumer.getReceiverQueueSize()).isEqualTo(2000);
        assertThat(consumer.getNegativeAckRedeliveryDelay()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Should configure transaction properties correctly")
    void shouldConfigureTransactionProperties() {
        PulsarProperties.Transaction transaction = properties.getTransaction();

        transaction.setEnabled(true);
        transaction.setTimeout(Duration.ofMinutes(2));
        transaction.setCoordinatorTopic("custom-coordinator-topic");
        transaction.setBufferSnapshotSegmentSize(2048 * 1024);
        transaction.setBufferSnapshotMinTimeInMillis(Duration.ofSeconds(10));
        transaction.setBufferSnapshotMaxTransactionCount(2000);
        transaction.setLogStoreSize(2L * 1024 * 1024 * 1024);

        assertThat(transaction.isEnabled()).isTrue();
        assertThat(transaction.getTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(transaction.getCoordinatorTopic()).isEqualTo("custom-coordinator-topic");
        assertThat(transaction.getBufferSnapshotSegmentSize()).isEqualTo(2048 * 1024);
        assertThat(transaction.getBufferSnapshotMinTimeInMillis()).isEqualTo(Duration.ofSeconds(10));
        assertThat(transaction.getBufferSnapshotMaxTransactionCount()).isEqualTo(2000);
        assertThat(transaction.getLogStoreSize()).isEqualTo(2L * 1024 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should configure dead letter queue properties correctly")
    void shouldConfigureDeadLetterQueueProperties() {
        PulsarProperties.DeadLetterQueueProperties deadLetter = new PulsarProperties.DeadLetterQueueProperties();
        properties.setDeadLetter(deadLetter);

        deadLetter.setTopicSuffix("-CUSTOM-DLQ");
        deadLetter.setMaxRetries(5);

        // Configure retry properties
        PulsarProperties.DeadLetterQueueProperties.Retry retry = deadLetter.getRetry();
        retry.setSmartStrategyEnabled(false);
        retry.setBaseDelay(Duration.ofSeconds(2));
        retry.setMaxDelay(Duration.ofMinutes(10));
        retry.setRetryWindow(Duration.ofHours(48));
        retry.setJitterEnabled(false);
        retry.setJitterFactor(0.3);

        assertThat(deadLetter.getTopicSuffix()).isEqualTo("-CUSTOM-DLQ");
        assertThat(deadLetter.getMaxRetries()).isEqualTo(5);
        assertThat(retry.isSmartStrategyEnabled()).isFalse();
        assertThat(retry.getBaseDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(retry.getMaxDelay()).isEqualTo(Duration.ofMinutes(10));
        assertThat(retry.getRetryWindow()).isEqualTo(Duration.ofHours(48));
        assertThat(retry.isJitterEnabled()).isFalse();
        assertThat(retry.getJitterFactor()).isEqualTo(0.3);
    }
}