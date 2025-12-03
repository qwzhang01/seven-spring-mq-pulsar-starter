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

package com.github.spring.mq.pulsar.exception;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;
import com.github.spring.mq.pulsar.annotation.ConsumerExceptionResponse;
import com.github.spring.mq.pulsar.domain.ConsumerExceptionResponseAction;
import com.github.spring.mq.pulsar.tracing.ConsumeExceptionHandlerContainer;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for exception handling functionality
 *
 * @author avinzhang
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Exception Handling Tests")
class ExceptionHandlingTest {

    @Mock
    private Consumer<byte[]> mockConsumer;

    @Mock
    private Message<byte[]> mockMessage;

    private ConsumeExceptionHandlerContainer exceptionHandlerContainer;
    private TestExceptionHandler testExceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandlerContainer = new ConsumeExceptionHandlerContainer();
        testExceptionHandler = new TestExceptionHandler();
    }

    @Test
    @DisplayName("Should register exception handlers correctly")
    void shouldRegisterExceptionHandlersCorrectly() throws Exception {
        Method ackMethod = TestExceptionHandler.class.getMethod("handleAckException", Exception.class);
        ConsumerExceptionHandler annotation = ackMethod.getAnnotation(ConsumerExceptionHandler.class);

        exceptionHandlerContainer.create(testExceptionHandler, ackMethod, annotation);

        // Setup mock consumer
        when(mockConsumer.isConnected()).thenReturn(true);

        // Trigger exception handling
        RuntimeException testException = new RuntimeException("Test exception");
        exceptionHandlerContainer.handle(mockConsumer, mockMessage, testException);

        assertThat(testExceptionHandler.isAckHandlerCalled()).isTrue();
        assertThat(testExceptionHandler.getHandledException()).isEqualTo(testException);

        // Verify ACK was called
        verify(mockConsumer).acknowledge(mockMessage);
    }

    @Test
    @DisplayName("Should handle NACK response action correctly")
    void shouldHandleNackResponseActionCorrectly() throws Exception {
        Method nackMethod = TestExceptionHandler.class.getMethod("handleNackException", Exception.class);
        ConsumerExceptionHandler annotation = nackMethod.getAnnotation(ConsumerExceptionHandler.class);

        exceptionHandlerContainer.create(testExceptionHandler, nackMethod, annotation);

        // Setup mock message
        when(mockMessage.getData()).thenReturn("test message".getBytes());

        // Trigger exception handling
        IllegalArgumentException testException = new IllegalArgumentException("Test exception");
        exceptionHandlerContainer.handle(mockConsumer, mockMessage, testException);

        assertThat(testExceptionHandler.isNackHandlerCalled()).isTrue();
        assertThat(testExceptionHandler.getHandledException()).isEqualTo(testException);

        // Verify NACK was called
        verify(mockConsumer).negativeAcknowledge(mockMessage);
    }

    @Test
    @DisplayName("Should handle RECONSUME_LATER response action correctly")
    void shouldHandleReconsumeLaterResponseActionCorrectly() throws Exception {
        Method reconsumeLaterMethod = TestExceptionHandler.class.getMethod("handleReconsumeLaterException", Exception.class);
        ConsumerExceptionHandler annotation = reconsumeLaterMethod.getAnnotation(ConsumerExceptionHandler.class);

        exceptionHandlerContainer.create(testExceptionHandler, reconsumeLaterMethod, annotation);

        // Setup mock message
        when(mockMessage.getData()).thenReturn("test message".getBytes());

        // Trigger exception handling
        PulsarConsumerLatterException testException = new PulsarConsumerLatterException("Test exception");
        exceptionHandlerContainer.handle(mockConsumer, mockMessage, testException);

        assertThat(testExceptionHandler.isReconsumeLaterHandlerCalled()).isTrue();
        assertThat(testExceptionHandler.getHandledException()).isEqualTo(testException);

        // Verify reconsumeLater was called
        verify(mockConsumer).reconsumeLater(eq(mockMessage), eq(60L), any());
    }

    @Test
    @DisplayName("Should handle exception hierarchy correctly")
    void shouldHandleExceptionHierarchyCorrectly() throws Exception {
        // Register handler for RuntimeException
        Method runtimeExceptionMethod = TestExceptionHandler.class.getMethod("handleRuntimeException", RuntimeException.class);
        ConsumerExceptionHandler annotation = runtimeExceptionMethod.getAnnotation(ConsumerExceptionHandler.class);
        exceptionHandlerContainer.create(testExceptionHandler, runtimeExceptionMethod, annotation);

        // Setup mock message
        when(mockMessage.getData()).thenReturn("test message".getBytes());

        // Trigger with IllegalArgumentException (subclass of RuntimeException)
        IllegalArgumentException testException = new IllegalArgumentException("Test exception");
        exceptionHandlerContainer.handle(mockConsumer, mockMessage, testException);

        assertThat(testExceptionHandler.isRuntimeExceptionHandlerCalled()).isTrue();
        assertThat(testExceptionHandler.getHandledException()).isEqualTo(testException);
    }

    @Test
    @DisplayName("Should use default exception handler when no specific handler found")
    void shouldUseDefaultExceptionHandlerWhenNoSpecificHandlerFound() throws Exception {
        // Register default exception handler
        Method defaultMethod = TestExceptionHandler.class.getMethod("handleDefaultException", Exception.class);
        ConsumerExceptionHandler annotation = defaultMethod.getAnnotation(ConsumerExceptionHandler.class);
        exceptionHandlerContainer.create(testExceptionHandler, defaultMethod, annotation);

        // Setup mock message
        when(mockMessage.getData()).thenReturn("test message".getBytes());

        // Trigger with any exception
        CustomException testException = new CustomException("Custom exception");
        exceptionHandlerContainer.handle(mockConsumer, mockMessage, testException);

        assertThat(testExceptionHandler.isDefaultHandlerCalled()).isTrue();
        assertThat(testExceptionHandler.getHandledException()).isEqualTo(testException);
    }

    // Test exception handler class
    public static class TestExceptionHandler {
        private final AtomicBoolean ackHandlerCalled = new AtomicBoolean(false);
        private final AtomicBoolean nackHandlerCalled = new AtomicBoolean(false);
        private final AtomicBoolean reconsumeLaterHandlerCalled = new AtomicBoolean(false);
        private final AtomicBoolean runtimeExceptionHandlerCalled = new AtomicBoolean(false);
        private final AtomicBoolean defaultHandlerCalled = new AtomicBoolean(false);
        private final AtomicReference<Throwable> handledException = new AtomicReference<>();

        @ConsumerExceptionHandler(RuntimeException.class)
        @ConsumerExceptionResponse(ConsumerExceptionResponseAction.ACK)
        public void handleAckException(Exception exception) {
            ackHandlerCalled.set(true);
            handledException.set(exception);
        }

        @ConsumerExceptionHandler(IllegalArgumentException.class)
        @ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
        public void handleNackException(Exception exception) {
            nackHandlerCalled.set(true);
            handledException.set(exception);
        }

        @ConsumerExceptionHandler(PulsarConsumerLatterException.class)
        @ConsumerExceptionResponse(ConsumerExceptionResponseAction.RECONSUME_LATER)
        public void handleReconsumeLaterException(Exception exception) {
            reconsumeLaterHandlerCalled.set(true);
            handledException.set(exception);
        }

        @ConsumerExceptionHandler(RuntimeException.class)
        @ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
        public void handleRuntimeException(RuntimeException exception) {
            runtimeExceptionHandlerCalled.set(true);
            handledException.set(exception);
        }

        @ConsumerExceptionHandler(Exception.class)
        @ConsumerExceptionResponse(ConsumerExceptionResponseAction.NACK)
        public void handleDefaultException(Exception exception) {
            defaultHandlerCalled.set(true);
            handledException.set(exception);
        }

        // Getters
        public boolean isAckHandlerCalled() {
            return ackHandlerCalled.get();
        }

        public boolean isNackHandlerCalled() {
            return nackHandlerCalled.get();
        }

        public boolean isReconsumeLaterHandlerCalled() {
            return reconsumeLaterHandlerCalled.get();
        }

        public boolean isRuntimeExceptionHandlerCalled() {
            return runtimeExceptionHandlerCalled.get();
        }

        public boolean isDefaultHandlerCalled() {
            return defaultHandlerCalled.get();
        }

        public Throwable getHandledException() {
            return handledException.get();
        }
    }

    // Custom exception for testing
    public static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }
}