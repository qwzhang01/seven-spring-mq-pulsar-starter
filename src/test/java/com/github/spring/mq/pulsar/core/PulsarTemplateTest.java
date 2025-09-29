package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.TestBase;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

/**
 * Tests for PulsarTemplate core functionality
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("Pulsar Template Tests")
class PulsarTemplateTest extends TestBase {

    private PulsarTemplate pulsarTemplate;
    private PulsarProperties pulsarProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        pulsarProperties = new PulsarProperties();
        pulsarProperties.setServiceUrl(getPulsarServiceUrl());
        pulsarProperties.getProducer().setTopic("test-topic");
        pulsarProperties.getConsumer().setTopic("test-topic");

        objectMapper = new ObjectMapper();
        DeadLetterMessageProcessor deadLetterProcessor = new DeadLetterMessageProcessor();

        pulsarTemplate = new PulsarTemplate(
                getTestPulsarClient(),
                pulsarProperties,
                objectMapper,
                deadLetterProcessor
        );
    }

    @Test
    @DisplayName("Should send message synchronously")
    void shouldSendMessageSynchronously() throws PulsarClientException {
        String message = "Hello, Pulsar!";
        String topic = "test-sync-topic";

        MessageId messageId = pulsarTemplate.send(topic, message);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should send message synchronously with key")
    void shouldSendMessageSynchronouslyWithKey() throws PulsarClientException {
        String message = "Hello, Pulsar with key!";
        String topic = "test-sync-key-topic";
        String key = "test-key";

        MessageId messageId = pulsarTemplate.send(topic, key, message);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should send message asynchronously")
    void shouldSendMessageAsynchronously() {
        String message = "Hello, Async Pulsar!";
        String topic = "test-async-topic";

        CompletableFuture<MessageId> future = pulsarTemplate.sendAsync(topic, message);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> future.isDone() && !future.isCompletedExceptionally());

        assertThat(future).isCompleted();
        assertThat(future.join()).isNotNull();
    }

    @Test
    @DisplayName("Should send message asynchronously with key")
    void shouldSendMessageAsynchronouslyWithKey() {
        String message = "Hello, Async Pulsar with key!";
        String topic = "test-async-key-topic";
        String key = "async-key";

        CompletableFuture<MessageId> future = pulsarTemplate.sendAsync(topic, key, message);

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> future.isDone() && !future.isCompletedExceptionally());

        assertThat(future).isCompleted();
        assertThat(future.join()).isNotNull();
    }

    @Test
    @DisplayName("Should send delayed message")
    void shouldSendDelayedMessage() throws PulsarClientException {
        String message = "Hello, Delayed Pulsar!";
        String topic = "test-delayed-topic";
        long delay = 1;
        TimeUnit unit = TimeUnit.SECONDS;

        MessageId messageId = pulsarTemplate.sendAfter(topic, message, delay, unit);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should send delayed message with key")
    void shouldSendDelayedMessageWithKey() throws PulsarClientException {
        String message = "Hello, Delayed Pulsar with key!";
        String topic = "test-delayed-key-topic";
        String key = "delayed-key";
        long delay = 1;
        TimeUnit unit = TimeUnit.SECONDS;

        MessageId messageId = pulsarTemplate.sendAfter(topic, key, message, delay, unit);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should send message at specific timestamp")
    void shouldSendMessageAtTimestamp() throws PulsarClientException {
        String message = "Hello, Scheduled Pulsar!";
        String topic = "test-scheduled-topic";
        long timestamp = System.currentTimeMillis() + 2000; // 2 seconds from now

        MessageId messageId = pulsarTemplate.sendAt(topic, message, timestamp);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should send message at specific timestamp with key")
    void shouldSendMessageAtTimestampWithKey() throws PulsarClientException {
        String message = "Hello, Scheduled Pulsar with key!";
        String topic = "test-scheduled-key-topic";
        String key = "scheduled-key";
        long timestamp = System.currentTimeMillis() + 2000; // 2 seconds from now

        MessageId messageId = pulsarTemplate.sendAt(topic, key, message, timestamp);

        assertThat(messageId).isNotNull();
    }

    @Test
    @DisplayName("Should serialize and deserialize string messages")
    void shouldSerializeAndDeserializeStringMessages() {
        String originalMessage = "Test string message";

        String deserializedMessage = pulsarTemplate.deserialize(
                originalMessage.getBytes(),
                null,
                String.class
        );

        assertThat(deserializedMessage).isEqualTo(originalMessage);
    }

    @Test
    @DisplayName("Should serialize and deserialize JSON messages")
    void shouldSerializeAndDeserializeJsonMessages() {
        TestMessage originalMessage = new TestMessage("test", 123);

        try {
            byte[] serialized = objectMapper.writeValueAsBytes(originalMessage);
            TestMessage deserializedMessage = pulsarTemplate.deserialize(
                    serialized,
                    null,
                    TestMessage.class
            );

            assertThat(deserializedMessage.getName()).isEqualTo(originalMessage.getName());
            assertThat(deserializedMessage.getValue()).isEqualTo(originalMessage.getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should handle message routing correctly")
    void shouldHandleMessageRoutingCorrectly() {
        // Test message routing logic
        String jsonMessage = "{\"msgRoute\":\"route1\",\"data\":\"test data\"}";
        java.util.Map<String, String> businessMap = java.util.Map.of("route1", "msgRoute");

        String route = pulsarTemplate.deserializeMsgRoute(jsonMessage.getBytes(), businessMap);

        assertThat(route).isEqualTo("route1");
    }

    @Test
    @DisplayName("Should close resources properly")
    void shouldCloseResourcesProperly() {
        // This test ensures that close() method doesn't throw exceptions
        assertThatCode(() -> pulsarTemplate.close()).doesNotThrowAnyException();
    }

    // Test message class for JSON serialization tests
    public static class TestMessage {
        private String name;
        private int value;

        public TestMessage() {
        }

        public TestMessage(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}