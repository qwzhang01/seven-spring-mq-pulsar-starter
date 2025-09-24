package com.github.spring.mq.pulsar.interceptor;

import com.github.spring.mq.pulsar.domain.MsgContext;
import com.github.spring.mq.pulsar.domain.MsgMetaKey;
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
 * capabilities for Pulsar message processing. It:
 * <ul>
 *   <li>Extracts trace context from incoming messages</li>
 *   <li>Injects trace context into outgoing messages</li>
 *   <li>Creates spans for message send/receive operations</li>
 *   <li>Propagates traceId and spanId through message headers</li>
 *   <li>Sets MDC context for logging</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.2.16
 */
public class TracingPulsarMessageInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingPulsarMessageInterceptor.class);

    private final Tracer tracer;

    public TracingPulsarMessageInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Object beforeSend(String topic, Object message) {
        if (tracer == null) {
            return message;
        }

        try {
            // Create a new span for message sending
            Span span = tracer.nextSpan()
                    .name("pulsar.send")
                    .tag("messaging.system", "pulsar")
                    .tag("messaging.destination", topic)
                    .tag("messaging.operation", "send")
                    .start();

            TraceContext traceContext = span.context();

            // Set trace information in MsgContext
            MsgContext.setTraceId(traceContext.traceId());
            MsgContext.setSpanId(traceContext.spanId());

            // Set MDC for logging
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

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        if (tracer == null) {
            return;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
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
            // Clear MDC
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        if (tracer == null) {
            return true;
        }

        try {
            // Extract trace context from message properties
            Map<String, String> properties = message.getProperties();
            String traceId = properties.get(MsgMetaKey.TRACE.getCode());
            String spanId = properties.get(MsgMetaKey.SPAN.getCode());

            Span span;
            if (StringUtils.hasText(traceId) && StringUtils.hasText(spanId)) {
                // Continue existing trace - create child span
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
                // Start new trace
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

            // Set trace information in MsgContext
            MsgContext.setTraceId(traceContext.traceId());
            MsgContext.setSpanId(traceContext.spanId());

            // Set MDC for logging
            MDC.put("traceId", traceContext.traceId());
            MDC.put("spanId", traceContext.spanId());

            return true;
        } catch (Exception e) {
            logger.warn("Failed to create tracing span for message receive", e);
            return true;
        }
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        if (tracer == null) {
            return;
        }

        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
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
            // Clear MDC and context
            MDC.remove("traceId");
            MDC.remove("spanId");
            MsgContext.remove();
        }
    }

    @Override
    public int getOrder() {
        // High priority to ensure tracing is set up first
        return -1000;
    }
}