package com.github.spring.mq.pulsar.annotation;

import com.github.spring.mq.pulsar.TestBase;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for @PulsarListener annotation processing
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest(classes = {PulsarListenerTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "spring.pulsar.enabled=true",
        "spring.pulsar.consumer.topic=test-listener-topic",
        "spring.pulsar.consumer.subscription-name=test-listener-subscription"
})
@DisplayName("Pulsar Listener Annotation Tests")
class PulsarListenerTest extends TestBase {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @Autowired
    private TestMessageListener testMessageListener;

    @Test
    @DisplayName("Should process @PulsarListener annotated methods")
    void shouldProcessPulsarListenerAnnotatedMethods() throws Exception {
        String testMessage = "Hello from PulsarListener test!";
        String topic = "test-listener-topic";

        // Send a message
        MessageId messageId = pulsarTemplate.send(topic, testMessage);
        assertThat(messageId).isNotNull();

        // Wait for message to be received and processed
        boolean messageReceived = testMessageListener.getLatch().await(10, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(testMessageListener.getReceivedMessage()).isEqualTo(testMessage);
    }

    @Test
    @DisplayName("Should handle message routing correctly")
    void shouldHandleMessageRoutingCorrectly() throws Exception {
        String routedMessage = "{\"msgRoute\":\"test-route\",\"data\":\"routed message\"}";
        String topic = "test-listener-topic";

        // Send a routed message
        MessageId messageId = pulsarTemplate.send(topic, routedMessage);
        assertThat(messageId).isNotNull();

        // Wait for message to be received and processed
        boolean messageReceived = testMessageListener.getRoutedLatch().await(10, TimeUnit.SECONDS);
        assertThat(messageReceived).isTrue();
        assertThat(testMessageListener.getReceivedRoutedMessage()).contains("routed message");
    }

    @Configuration
    @EnablePulsar
    static class TestConfiguration {

        @Bean
        public TestMessageListener testMessageListener() {
            return new TestMessageListener();
        }
    }

    public static class TestMessageListener {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final CountDownLatch routedLatch = new CountDownLatch(1);
        private final AtomicReference<String> receivedMessage = new AtomicReference<>();
        private final AtomicReference<String> receivedRoutedMessage = new AtomicReference<>();

        @PulsarListener(topic = "test-listener-topic")
        public void handleMessage(String message) {
            receivedMessage.set(message);
            latch.countDown();
        }

        @PulsarListener(topic = "test-listener-topic", msgRoute = "test-route")
        public void handleRoutedMessage(String message) {
            receivedRoutedMessage.set(message);
            routedLatch.countDown();
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public CountDownLatch getRoutedLatch() {
            return routedLatch;
        }

        public String getReceivedMessage() {
            return receivedMessage.get();
        }

        public String getReceivedRoutedMessage() {
            return receivedRoutedMessage.get();
        }
    }
}