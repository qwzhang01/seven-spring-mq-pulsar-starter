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

package com.github.spring.mq.pulsar.interceptor;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for message interceptors
 *
 * @author avinzhang
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Message Interceptor Tests")
class InterceptorTest {

    @Mock
    private Message<byte[]> mockMessage;

    @Mock
    private MessageId mockMessageId;

    private TestPulsarMessageInterceptor testInterceptor;

    @BeforeEach
    void setUp() {
        testInterceptor = new TestPulsarMessageInterceptor();
        // Removed unnecessary stubbing - will be set up in individual tests as needed
    }

    @Test
    @DisplayName("Should execute beforeSend interceptor")
    void shouldExecuteBeforeSendInterceptor() {
        String topic = "test-topic";
        String message = "test message";

        Object result = testInterceptor.beforeSend(topic, message);

        assertThat(testInterceptor.isBeforeSendCalled()).isTrue();
        assertThat(testInterceptor.getBeforeSendTopic()).isEqualTo(topic);
        assertThat(testInterceptor.getBeforeSendMessage()).isEqualTo(message);
        assertThat(result).isEqualTo(message);
    }

    @Test
    @DisplayName("Should execute afterSend interceptor")
    void shouldExecuteAfterSendInterceptor() {
        String topic = "test-topic";
        String message = "test message";
        Exception exception = new RuntimeException("test exception");

        testInterceptor.afterSend(topic, message, mockMessageId, exception);

        assertThat(testInterceptor.isAfterSendCalled()).isTrue();
        assertThat(testInterceptor.getAfterSendTopic()).isEqualTo(topic);
        assertThat(testInterceptor.getAfterSendMessage()).isEqualTo(message);
        assertThat(testInterceptor.getAfterSendMessageId()).isEqualTo(mockMessageId);
        assertThat(testInterceptor.getAfterSendException()).isEqualTo(exception);
    }

    @Test
    @DisplayName("Should execute beforeReceive interceptor")
    void shouldExecuteBeforeReceiveInterceptor() {
        boolean result = testInterceptor.beforeReceive(mockMessage);

        assertThat(testInterceptor.isBeforeReceiveCalled()).isTrue();
        assertThat(testInterceptor.getBeforeReceiveMessage()).isEqualTo(mockMessage);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should execute afterReceive interceptor")
    void shouldExecuteAfterReceiveInterceptor() {
        String processedMessage = "processed message";
        Exception exception = new RuntimeException("test exception");

        testInterceptor.afterReceive(mockMessage, processedMessage, exception);

        assertThat(testInterceptor.isAfterReceiveCalled()).isTrue();
        assertThat(testInterceptor.getAfterReceiveMessage()).isEqualTo(mockMessage);
        assertThat(testInterceptor.getAfterReceiveProcessedMessage()).isEqualTo(processedMessage);
        assertThat(testInterceptor.getAfterReceiveException()).isEqualTo(exception);
    }

    @Test
    @DisplayName("Should allow message filtering in beforeSend")
    void shouldAllowMessageFilteringInBeforeSend() {
        TestFilteringInterceptor filteringInterceptor = new TestFilteringInterceptor();

        // First call should return null (filter out)
        Object result1 = filteringInterceptor.beforeSend("test-topic", "filter-me");
        assertThat(result1).isNull();

        // Second call should return the message
        Object result2 = filteringInterceptor.beforeSend("test-topic", "allow-me");
        assertThat(result2).isEqualTo("allow-me");
    }

    @Test
    @DisplayName("Should allow message filtering in beforeReceive")
    void shouldAllowMessageFilteringInBeforeReceive() {
        TestFilteringInterceptor filteringInterceptor = new TestFilteringInterceptor();

        // Mock message with filtering data
        when(mockMessage.getData()).thenReturn("filter-me".getBytes());
        boolean result1 = filteringInterceptor.beforeReceive(mockMessage);
        assertThat(result1).isFalse();

        // Reset and mock message with allowed data
        when(mockMessage.getData()).thenReturn("allow-me".getBytes());
        boolean result2 = filteringInterceptor.beforeReceive(mockMessage);
        assertThat(result2).isTrue();
    }

    // Test interceptor implementation
    public static class TestPulsarMessageInterceptor implements PulsarMessageInterceptor {
        private final AtomicBoolean beforeSendCalled = new AtomicBoolean(false);
        private final AtomicBoolean afterSendCalled = new AtomicBoolean(false);
        private final AtomicBoolean beforeReceiveCalled = new AtomicBoolean(false);
        private final AtomicBoolean afterReceiveCalled = new AtomicBoolean(false);

        private final AtomicReference<String> beforeSendTopic = new AtomicReference<>();
        private final AtomicReference<Object> beforeSendMessage = new AtomicReference<>();
        private final AtomicReference<String> afterSendTopic = new AtomicReference<>();
        private final AtomicReference<Object> afterSendMessage = new AtomicReference<>();
        private final AtomicReference<MessageId> afterSendMessageId = new AtomicReference<>();
        private final AtomicReference<Throwable> afterSendException = new AtomicReference<>();
        private final AtomicReference<Message<?>> beforeReceiveMessage = new AtomicReference<>();
        private final AtomicReference<Message<?>> afterReceiveMessage = new AtomicReference<>();
        private final AtomicReference<Object> afterReceiveProcessedMessage = new AtomicReference<>();
        private final AtomicReference<Exception> afterReceiveException = new AtomicReference<>();

        @Override
        public Object beforeSend(String topic, Object message) {
            beforeSendCalled.set(true);
            beforeSendTopic.set(topic);
            beforeSendMessage.set(message);
            return message;
        }

        @Override
        public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
            afterSendCalled.set(true);
            afterSendTopic.set(topic);
            afterSendMessage.set(message);
            afterSendMessageId.set(messageId);
            afterSendException.set(exception);
        }

        @Override
        public boolean beforeReceive(Message<?> message) {
            beforeReceiveCalled.set(true);
            beforeReceiveMessage.set(message);
            return true;
        }

        @Override
        public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
            afterReceiveCalled.set(true);
            afterReceiveMessage.set(message);
            afterReceiveProcessedMessage.set(processedMessage);
            afterReceiveException.set(exception);
        }

        // Getters
        public boolean isBeforeSendCalled() {
            return beforeSendCalled.get();
        }

        public boolean isAfterSendCalled() {
            return afterSendCalled.get();
        }

        public boolean isBeforeReceiveCalled() {
            return beforeReceiveCalled.get();
        }

        public boolean isAfterReceiveCalled() {
            return afterReceiveCalled.get();
        }

        public String getBeforeSendTopic() {
            return beforeSendTopic.get();
        }

        public Object getBeforeSendMessage() {
            return beforeSendMessage.get();
        }

        public String getAfterSendTopic() {
            return afterSendTopic.get();
        }

        public Object getAfterSendMessage() {
            return afterSendMessage.get();
        }

        public MessageId getAfterSendMessageId() {
            return afterSendMessageId.get();
        }

        public Throwable getAfterSendException() {
            return afterSendException.get();
        }

        public Message<?> getBeforeReceiveMessage() {
            return beforeReceiveMessage.get();
        }

        public Message<?> getAfterReceiveMessage() {
            return afterReceiveMessage.get();
        }

        public Object getAfterReceiveProcessedMessage() {
            return afterReceiveProcessedMessage.get();
        }

        public Exception getAfterReceiveException() {
            return afterReceiveException.get();
        }
    }

    // Test filtering interceptor
    public static class TestFilteringInterceptor implements PulsarMessageInterceptor {
        @Override
        public Object beforeSend(String topic, Object message) {
            if (message.toString().contains("filter-me")) {
                return null; // Filter out this message
            }
            return message;
        }

        @Override
        public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
            // No-op for this test
        }

        @Override
        public boolean beforeReceive(Message<?> message) {
            String messageContent = new String(message.getData());
            return !messageContent.contains("filter-me");
        }

        @Override
        public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
            // No-op for this test
        }
    }
}