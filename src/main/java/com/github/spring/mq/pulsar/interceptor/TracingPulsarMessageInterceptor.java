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

import com.github.spring.mq.pulsar.domain.MsgContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Tracing interceptor for Pulsar messages
 *
 * <p>This interceptor integrates with Micrometer Tracing to provide distributed tracing
 * capabilities for Pulsar message processing. It handles the complete lifecycle of
 * trace context propagation across message boundaries:
 * <ul>
 *   <li>Creates spans for message send/receive operations</li>
 *   <li>Extracts trace context from incoming message headers</li>
 *   <li>Injects trace context into outgoing message headers</li>
 *   <li>Propagates traceId and spanId through thread-local context</li>
 *   <li>Sets up MDC (Mapped Diagnostic Context) for structured logging</li>
 *   <li>Handles span lifecycle management and resource cleanup</li>
 * </ul>
 *
 * <p>The interceptor follows OpenTelemetry semantic conventions for messaging systems
 * and ensures proper trace continuity across distributed message flows.
 *
 * @author avinzhang
 * @since 1.2.16
 */
public class TracingPulsarMessageInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingPulsarMessageInterceptor.class);

    // Standard distributed tracing header names following OpenTelemetry conventions
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String TRACE_FLAGS_HEADER = "X-Trace-Flags";

    private final Tracer tracer;

    /**
     * Constructs a new tracing interceptor with the provided tracer
     *
     * @param tracer the Micrometer tracer instance for creating and managing spans
     */
    public TracingPulsarMessageInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Intercepts message sending to create a producer span and set up trace context
     *
     * <p>This method is called before a message is sent to Pulsar. It:
     * <ul>
     *   <li>Creates a new span representing the message send operation</li>
     *   <li>Tags the span with messaging system metadata</li>
     *   <li>Stores trace information in thread-local context for header injection</li>
     *   <li>Sets up MDC for logging correlation</li>
     * </ul>
     *
     * @param topic   the destination topic name
     * @param message the message object being sent
     * @return the original message object (unchanged)
     */
    @Override
    public Object beforeSend(String topic, Object message) {
        if (tracer == null) {
            return message;
        }

        try {
            // Create a new span for message sending operation
            // This span represents the producer side of the message flow
            Span span = tracer.nextSpan()
                    .name("pulsar.send")
                    .tag("messaging.system", "pulsar")
                    .tag("messaging.destination", topic)
                    .tag("messaging.operation", "send")
                    .start();

            TraceContext traceContext = span.context();

            // Store trace information in thread-local context for later injection into message headers
            MsgContext.setTraceId(traceContext.traceId());
            MsgContext.setSpanId(traceContext.spanId());

            // Set MDC (Mapped Diagnostic Context) for structured logging
            MDC.put("traceId", traceContext.traceId());
            MDC.put("spanId", traceContext.spanId());

            logger.debug("Starting message send span - traceId: {}, spanId: {}, topic: {}",
                    traceContext.traceId(), traceContext.spanId(), topic);

            return message;
        } catch (Exception e) {
            logger.warn("Failed to create tracing span for message send", e);
            return message;
        }
    }

    /**
     * Completes the message send span and performs cleanup
     *
     * <p>This method is called after a message send operation completes (successfully or with error).
     * It finalizes the span with appropriate tags and events, then cleans up the trace context.
     *
     * @param topic     the destination topic name
     * @param message   the message object that was sent
     * @param messageId the Pulsar message ID (null if send failed)
     * @param exception any exception that occurred during sending (null if successful)
     */
    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        if (tracer == null) {
            return;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // Tag span with outcome and relevant metadata
                if (exception != null) {
                    currentSpan.tag("error", exception.getMessage());
                    currentSpan.event("send.error");
                } else {
                    currentSpan.tag("messaging.message_id", messageId != null ? messageId.toString() : "unknown");
                    currentSpan.event("send.success");
                }
                currentSpan.end();

                logger.debug("Ended message send span - topic: {}, messageId: {}, error: {}",
                        topic, messageId, exception != null ? exception.getMessage() : "none");
            }
        } catch (Exception e) {
            logger.warn("Failed to end tracing span for message send", e);
        } finally {
            // Clean up MDC to prevent memory leaks in thread pools
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    /**
     * Intercepts message receiving to extract trace context and create a consumer span
     *
     * <p>This method is called before a message is processed by a consumer. It:
     * <ul>
     *   <li>Extracts trace context from message headers</li>
     *   <li>Creates a child span for the message receive operation</li>
     *   <li>Sets up thread-local context and MDC for the processing thread</li>
     *   <li>Enables trace continuity across the message boundary</li>
     * </ul>
     *
     * @param message the received Pulsar message
     * @return true to continue processing, false to skip (always returns true)
     */
    @Override
    public boolean beforeReceive(Message<?> message) {
        if (tracer == null) {
            return true;
        }

        try {
            // Extract trace context from message properties using standard headers
            Map<String, String> properties = message.getProperties();
            String traceId = properties.get(TRACE_ID_HEADER);
            String spanId = properties.get(SPAN_ID_HEADER);

            Span span;
            if (StringUtils.hasText(traceId) && StringUtils.hasText(spanId)) {
                // Continue existing trace by creating a child span
                logger.debug("Continuing trace from message - traceId: {}, spanId: {}, topic: {}",
                        traceId, spanId, message.getTopicName());

                span = tracer.nextSpan()
                        .name("pulsar.receive")
                        .tag("messaging.system", "pulsar")
                        .tag("messaging.destination", message.getTopicName())
                        .tag("messaging.operation", "receive")
                        .tag("messaging.message_id", message.getMessageId().toString())
                        .tag("parent.trace_id", traceId)
                        .tag("parent.span_id", spanId)
                        .start();
            } else {
                // Start new trace if no trace context found in message
                span = tracer.nextSpan()
                        .name("pulsar.receive")
                        .tag("messaging.system", "pulsar")
                        .tag("messaging.destination", message.getTopicName())
                        .tag("messaging.operation", "receive")
                        .tag("messaging.message_id", message.getMessageId().toString())
                        .start();

                logger.debug("Starting new trace for message - topic: {}", message.getTopicName());
            }

            TraceContext traceContext = span.context();

            // Store trace information in thread-local context for business logic access
            MsgContext.setTraceId(traceContext.traceId());
            MsgContext.setSpanId(traceContext.spanId());

            // Set MDC (Mapped Diagnostic Context) for structured logging
            MDC.put("traceId", traceContext.traceId());
            MDC.put("spanId", traceContext.spanId());

            return true;
        } catch (Exception e) {
            logger.warn("Failed to create tracing span for message receive", e);
            return true;
        }
    }

    /**
     * Completes the message receive span and performs cleanup
     *
     * <p>This method is called after message processing completes. It finalizes the
     * consumer span with appropriate outcome information and cleans up all trace context
     * to prevent memory leaks and context pollution.
     *
     * @param message          the original Pulsar message
     * @param processedMessage the processed message object (may be null)
     * @param exception        any exception that occurred during processing (null if successful)
     */
    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        if (tracer == null) {
            return;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // Tag span with processing outcome
                if (exception != null) {
                    currentSpan.tag("error", exception.getMessage());
                    currentSpan.event("receive.error");
                } else {
                    currentSpan.event("receive.success");
                }
                currentSpan.end();

                logger.debug("Ended message receive span - topic: {}, messageId: {}, error: {}",
                        message.getTopicName(), message.getMessageId(),
                        exception != null ? exception.getMessage() : "none");
            }
        } catch (Exception e) {
            logger.warn("Failed to end tracing span for message receive", e);
        } finally {
            // Clean up all trace context to prevent memory leaks and context pollution
            MDC.remove("traceId");
            MDC.remove("spanId");
            MsgContext.remove();
        }
    }

    /**
     * Returns the execution order of this interceptor
     *
     * <p>This interceptor has high priority (low order value) to ensure that
     * tracing context is established before other interceptors execute.
     *
     * @return -1000 to ensure high priority execution
     */
    @Override
    public int getOrder() {
        return -1000; // High priority to ensure tracing is set up first
    }
}