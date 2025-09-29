package com.github.spring.mq.pulsar.integration;

import com.github.spring.mq.pulsar.TestBase;
import com.github.spring.mq.pulsar.annotation.EnablePulsar;
import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete Pulsar functionality
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest(classes = {PulsarIntegrationTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "spring.pulsar.enabled=true",
        "spring.pulsar.producer.topic=integration-test-topic",
        "spring.pulsar.consumer.topic=integration-test-topic",
        "spring.pulsar.consumer.subscription-name=integration-test-subscription",
        "spring.pulsar.consumer.auto-ack=true"
})
@DisplayName("Pulsar Integration Tests")
class PulsarIntegrationTest extends TestBase {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @Autowired
    private IntegrationTestMessageListener messageListener;

    @Test
    @DisplayName("Should send and receive messages end-to-end")
    void shouldSendAndReceiveMessagesEndToEnd() throws Exception {
        String testMessage = "Integration test message";
        String topic = "integration-test-topic";

        // Send message
        MessageId messageId = pulsarTemplate.send(topic, testMessage);
        assertThat(messageId).isNotNull();

        // Wait for message to be received
        boolean messageReceived = messageListener.getLatch().await(15, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(messageListener.getReceivedMessage()).isEqualTo(testMessage);
    }

    @Test
    @DisplayName("Should handle multiple messages correctly")
    void shouldHandleMultipleMessagesCorrectly() throws Exception {
        String topic = "integration-test-topic";
        int messageCount = 5;

        // Reset counter
        messageListener.resetCounter();

        // Send multiple messages
        for (int i = 0; i < messageCount; i++) {
            String message = "Message " + i;
            MessageId messageId = pulsarTemplate.send(topic, message);
            assertThat(messageId).isNotNull();
        }

        // Wait for all messages to be received
        boolean allMessagesReceived = messageListener.getMultipleMessagesLatch(messageCount)
                .await(20, TimeUnit.SECONDS);
        assertThat(allMessagesReceived).isTrue();
        assertThat(messageListener.getMessageCount()).isEqualTo(messageCount);
    }

    @Test
    @DisplayName("Should handle async message sending")
    void shouldHandleAsyncMessageSending() throws Exception {
        String testMessage = "Async integration test message";
        String topic = "integration-test-topic";

        // Send message asynchronously
        var future = pulsarTemplate.sendAsync(topic, testMessage);

        // Wait for send to complete
        MessageId messageId = future.get(10, TimeUnit.SECONDS);
        assertThat(messageId).isNotNull();

        // Wait for message to be received
        boolean messageReceived = messageListener.getLatch().await(15, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(messageListener.getReceivedMessage()).isEqualTo(testMessage);
    }

    @Test
    @DisplayName("Should handle delayed message delivery")
    void shouldHandleDelayedMessageDelivery() throws Exception {
        String testMessage = "Delayed integration test message";
        String topic = "integration-test-topic";

        long startTime = System.currentTimeMillis();

        // Send delayed message (2 seconds delay)
        MessageId messageId = pulsarTemplate.sendAfter(topic, testMessage, 2, TimeUnit.SECONDS);
        assertThat(messageId).isNotNull();

        // Wait for message to be received
        boolean messageReceived = messageListener.getLatch().await(15, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(messageListener.getReceivedMessage()).isEqualTo(testMessage);

        // Verify delay (should be at least 2 seconds)
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(elapsedTime).isGreaterThanOrEqualTo(2000);
    }

    @Test
    @DisplayName("Should handle JSON message serialization")
    void shouldHandleJsonMessageSerialization() throws Exception {
        TestMessage testMessage = new TestMessage("integration-test", 42);
        String topic = "integration-test-topic";

        // Send JSON message
        MessageId messageId = pulsarTemplate.send(topic, testMessage);
        assertThat(messageId).isNotNull();

        // Wait for message to be received
        boolean messageReceived = messageListener.getJsonLatch().await(15, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();

        TestMessage receivedMessage = messageListener.getReceivedJsonMessage();
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getName()).isEqualTo(testMessage.getName());
        assertThat(receivedMessage.getValue()).isEqualTo(testMessage.getValue());
    }

    @Configuration
    @EnablePulsar
    static class TestConfiguration {

        @Bean
        public IntegrationTestMessageListener integrationTestMessageListener() {
            return new IntegrationTestMessageListener();
        }
    }

    public static class IntegrationTestMessageListener {
        private final AtomicReference<String> receivedMessage = new AtomicReference<>();
        private final AtomicReference<TestMessage> receivedJsonMessage = new AtomicReference<>();
        private final AtomicInteger messageCount = new AtomicInteger(0);
        private CountDownLatch latch = new CountDownLatch(1);
        private CountDownLatch jsonLatch = new CountDownLatch(1);
        private CountDownLatch multipleMessagesLatch;

        @PulsarListener(topic = "integration-test-topic", messageType = String.class)
        public void handleStringMessage(String message) {
            receivedMessage.set(message);
            messageCount.incrementAndGet();
            latch.countDown();
            if (multipleMessagesLatch != null) {
                multipleMessagesLatch.countDown();
            }
        }

        @PulsarListener(topic = "integration-test-topic", messageType = TestMessage.class)
        public void handleJsonMessage(TestMessage message) {
            receivedJsonMessage.set(message);
            jsonLatch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public CountDownLatch getJsonLatch() {
            return jsonLatch;
        }

        public String getReceivedMessage() {
            return receivedMessage.get();
        }

        public TestMessage getReceivedJsonMessage() {
            return receivedJsonMessage.get();
        }

        public int getMessageCount() {
            return messageCount.get();
        }

        public void resetCounter() {
            messageCount.set(0);
            latch = new CountDownLatch(1);
        }

        public CountDownLatch getMultipleMessagesLatch(int count) {
            multipleMessagesLatch = new CountDownLatch(count);
            return multipleMessagesLatch;
        }
    }

    // Test message class for JSON serialization
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