package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.domain.MsgMetaKey;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Utility class for propagating trace context through Pulsar message headers
 *
 * <p>This class provides methods to inject and extract trace context information
 * from Pulsar message properties. It supports standard trace propagation headers:
 * <ul>
 *   <li>X-Trace-Id: The trace identifier</li>
 *   <li>X-Span-Id: The span identifier</li>
 *   <li>X-Trace-Flags: Trace sampling flags</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.2.16
 */
public class PulsarMessageHeadersPropagator {

    private static final Logger logger = LoggerFactory.getLogger(PulsarMessageHeadersPropagator.class);

    /**
     * Inject trace context into message builder
     *
     * @param messageBuilder the message builder
     * @param traceId        the trace ID
     * @param spanId         the span ID
     * @param sampled        whether the trace is sampled
     */
    public static void injectTraceContext(TypedMessageBuilder<?> messageBuilder,
                                          String traceId,
                                          String spanId,
                                          boolean sampled) {
        if (messageBuilder == null) {
            return;
        }

        try {
            if (traceId != null && !traceId.isEmpty()) {
                messageBuilder.property(MsgMetaKey.TRACE.getCode(), traceId);
            }
            if (spanId != null && !spanId.isEmpty()) {
                messageBuilder.property(MsgMetaKey.SPAN.getCode(), spanId);
            }
            messageBuilder.property(MsgMetaKey.TRACE_FLAG.getCode(), sampled ? "01" : "00");

            logger.debug("Injected trace context - traceId: {}, spanId: {}, sampled: {}",
                    traceId, spanId, sampled);
        } catch (Exception e) {
            logger.warn("Failed to inject trace context into message", e);
        }
    }

    public static void injectCorp(TypedMessageBuilder<byte[]> messageBuilder, String corpKey, String appName, LocalDateTime time, String msgId) {
        if (messageBuilder == null) {
            return;
        }
        try {
            if (corpKey != null && !corpKey.isEmpty()) {
                messageBuilder.property(MsgMetaKey.CORP.getCode(), corpKey);
            }
            if (appName != null && !appName.isEmpty()) {
                messageBuilder.property(MsgMetaKey.APP.getCode(), appName);
            }
            if (time != null) {
                messageBuilder.property(MsgMetaKey.TIME.getCode(), time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (msgId != null && !msgId.isEmpty()) {
                messageBuilder.property(MsgMetaKey.MSG_ID.getCode(), msgId);
            }
            logger.debug("Injected corp context - corpKey: {}, appName: {}, time: {}, msgId: {}",
                    corpKey, appName, time, msgId);
        } catch (Exception e) {
            logger.warn("Failed to inject corp context into message", e);
        }
    }

    public static void injectMsgRoute(TypedMessageBuilder<byte[]> messageBuilder, String msgRoute) {
        if (messageBuilder == null) {
            return;
        }
        try {
            if (msgRoute == null || msgRoute.isEmpty()) {
                msgRoute = "";
            }
            messageBuilder.property(MsgMetaKey.MSG_ROUTE.getCode(), msgRoute);
            logger.debug("Injected msg route - msgRoute: {}",
                    msgRoute);
        } catch (Exception e) {
            logger.warn("Failed to inject msg route into message", e);
        }
    }

    /**
     * Extract trace context from message properties
     *
     * @param properties the message properties
     * @return trace context information
     */
    public static TraceContext extractTraceContext(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }

        try {
            String traceId = properties.get(MsgMetaKey.TRACE.getCode());
            String spanId = properties.get(MsgMetaKey.SPAN.getCode());
            String flags = properties.get(MsgMetaKey.TRACE_FLAG.getCode());

            if (traceId == null || spanId == null) {
                return null;
            }

            boolean sampled = "01".equals(flags);

            logger.debug("Extracted trace context - traceId: {}, spanId: {}, sampled: {}",
                    traceId, spanId, sampled);

            return new TraceContext(traceId, spanId, sampled);
        } catch (Exception e) {
            logger.warn("Failed to extract trace context from message properties", e);
            return null;
        }
    }

    public static String extractMsgRoute(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }

        try {
            return properties.get(MsgMetaKey.MSG_ROUTE.getCode());
        } catch (Exception e) {
            logger.warn("Failed to extract msg route from message properties", e);
            return null;
        }
    }

    /**
     * Simple trace context holder
     */
    public static class TraceContext {
        private final String traceId;
        private final String spanId;
        private final boolean sampled;

        public TraceContext(String traceId, String spanId, boolean sampled) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.sampled = sampled;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getSpanId() {
            return spanId;
        }

        public boolean isSampled() {
            return sampled;
        }

        @Override
        public String toString() {
            return "TraceContext{" +
                    "traceId='" + traceId + '\'' +
                    ", spanId='" + spanId + '\'' +
                    ", sampled=" + sampled +
                    '}';
        }
    }
}