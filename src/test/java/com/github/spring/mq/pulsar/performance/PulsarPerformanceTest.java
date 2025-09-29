package com.github.spring.mq.pulsar.performance;

import com.github.spring.mq.pulsar.TestBase;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for Pulsar functionality
 *
 * <p>These tests verify that the Pulsar starter can handle reasonable loads
 * and perform within acceptable time limits.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.pulsar.enabled=true",
        "spring.pulsar.producer.topic=performance-test-topic",
        "spring.pulsar.producer.batching-enabled=true",
        "spring.pulsar.producer.batching-max-messages=100",
        "spring.pulsar.producer.batching-max-publish-delay=10ms"
})
@DisplayName("Pulsar Performance Tests")
class PulsarPerformanceTest extends TestBase {

    @Autowired
    private PulsarTemplate pulsarTemplate;

    @Test
    @DisplayName("Should handle high-volume synchronous message sending")
    void shouldHandleHighVolumeSynchronousMessageSending() throws Exception {
        String topic = "performance-test-sync-topic";
        int messageCount = 1000;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            String message = "Performance test message " + i;
            MessageId messageId = pulsarTemplate.send(topic, message);
            assertThat(messageId).isNotNull();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within reasonable time (adjust threshold as needed)
        assertThat(duration).isLessThan(30000); // 30 seconds

        // Calculate throughput
        double throughput = (double) messageCount / (duration / 1000.0);
        System.out.printf("Synchronous throughput: %.2f messages/second%n", throughput);

        // Should achieve reasonable throughput (adjust threshold as needed)
        assertThat(throughput).isGreaterThan(10.0); // At least 10 messages per second
    }

    @Test
    @DisplayName("Should handle high-volume asynchronous message sending")
    void shouldHandleHighVolumeAsynchronousMessageSending() throws Exception {
        String topic = "performance-test-async-topic";
        int messageCount = 1000;

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<MessageId>> futures = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            String message = "Async performance test message " + i;
            CompletableFuture<MessageId> future = pulsarTemplate.sendAsync(topic, message);
            futures.add(future);
        }

        // Wait for all messages to be sent
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allFutures.get(60, TimeUnit.SECONDS); // Wait up to 60 seconds

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Verify all messages were sent successfully
        for (CompletableFuture<MessageId> future : futures) {
            assertThat(future).isCompleted();
            assertThat(future.join()).isNotNull();
        }

        // Should complete within reasonable time
        assertThat(duration).isLessThan(60000); // 60 seconds

        // Calculate throughput
        double throughput = (double) messageCount / (duration / 1000.0);
        System.out.printf("Asynchronous throughput: %.2f messages/second%n", throughput);

        // Async should be faster than sync
        assertThat(throughput).isGreaterThan(50.0); // At least 50 messages per second
    }

    @Test
    @DisplayName("Should handle concurrent message sending from multiple threads")
    void shouldHandleConcurrentMessageSendingFromMultipleThreads() throws Exception {
        String topic = "performance-test-concurrent-topic";
        int threadCount = 10;
        int messagesPerThread = 100;

        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> threadFutures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            CompletableFuture<Void> threadFuture = CompletableFuture.runAsync(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        String message = String.format("Thread %d message %d", threadId, i);
                        MessageId messageId = pulsarTemplate.send(topic, message);
                        assertThat(messageId).isNotNull();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threadFutures.add(threadFuture);
        }

        // Wait for all threads to complete
        CompletableFuture<Void> allThreads = CompletableFuture.allOf(
                threadFutures.toArray(new CompletableFuture[0])
        );

        allThreads.get(120, TimeUnit.SECONDS); // Wait up to 2 minutes

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        int totalMessages = threadCount * messagesPerThread;

        // Should complete within reasonable time
        assertThat(duration).isLessThan(120000); // 2 minutes

        // Calculate throughput
        double throughput = (double) totalMessages / (duration / 1000.0);
        System.out.printf("Concurrent throughput: %.2f messages/second (%d threads)%n",
                throughput, threadCount);

        // Should handle concurrent load reasonably well
        assertThat(throughput).isGreaterThan(5.0); // At least 5 messages per second
    }

    @Test
    @DisplayName("Should handle large message payloads efficiently")
    void shouldHandleLargeMessagePayloadsEfficiently() throws Exception {
        String topic = "performance-test-large-payload-topic";
        int messageCount = 100;
        int payloadSize = 10 * 1024; // 10KB per message

        // Create large payload
        StringBuilder payloadBuilder = new StringBuilder(payloadSize);
        for (int i = 0; i < payloadSize; i++) {
            payloadBuilder.append((char) ('A' + (i % 26)));
        }
        String largePayload = payloadBuilder.toString();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            String message = "Large message " + i + ": " + largePayload;
            MessageId messageId = pulsarTemplate.send(topic, message);
            assertThat(messageId).isNotNull();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within reasonable time even with large payloads
        assertThat(duration).isLessThan(60000); // 60 seconds

        // Calculate throughput in terms of data
        long totalBytes = (long) messageCount * largePayload.length();
        double throughputMBps = (double) totalBytes / (1024 * 1024) / (duration / 1000.0);
        System.out.printf("Large payload throughput: %.2f MB/second%n", throughputMBps);

        // Should achieve reasonable data throughput
        assertThat(throughputMBps).isGreaterThan(0.1); // At least 0.1 MB/second
    }

    @Test
    @DisplayName("Should maintain performance with message keys")
    void shouldMaintainPerformanceWithMessageKeys() throws Exception {
        String topic = "performance-test-keyed-topic";
        int messageCount = 500;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < messageCount; i++) {
            String key = "key-" + (i % 10); // Use 10 different keys
            String message = "Keyed message " + i;
            MessageId messageId = pulsarTemplate.send(topic, key, message);
            assertThat(messageId).isNotNull();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Should complete within reasonable time
        assertThat(duration).isLessThan(30000); // 30 seconds

        // Calculate throughput
        double throughput = (double) messageCount / (duration / 1000.0);
        System.out.printf("Keyed message throughput: %.2f messages/second%n", throughput);

        // Should maintain reasonable performance with keys
        assertThat(throughput).isGreaterThan(5.0); // At least 5 messages per second
    }
}