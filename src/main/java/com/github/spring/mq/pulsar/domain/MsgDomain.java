package com.github.spring.mq.pulsar.domain;

import java.time.LocalDateTime;

/**
 * Default message domain object
 *
 * <p>This is the standard message wrapper used by the Pulsar starter.
 * It provides a consistent structure for all messages with common
 * metadata fields and a generic data payload.
 *
 * <p>The message includes:
 * <ul>
 *   <li>Multi-tenancy support with corporation key</li>
 *   <li>Application identification</li>
 *   <li>Request tracing capabilities</li>
 *   <li>Message routing information</li>
 *   <li>Idempotency support with message ID</li>
 *   <li>Timestamp tracking</li>
 * </ul>
 *
 * @param <T> The type of the message data payload
 * @author avinzhang
 * @since 1.0.0
 */
public class MsgDomain<T> {
    /**
     * Multi-tenant corporation key
     */
    private String corpKey;
    /**
     * Application name
     */
    private String appName;
    /**
     * Request ID for tracing
     * Used as trace ID for logging
     */
    private String traceId;
    private String spanId;
    /**
     * Message ID for idempotency
     * Used to ensure message processing idempotency
     */
    private String msgId;
    /**
     * Message business route
     */
    private String msgRoute;
    /**
     * Message creation time
     */
    private LocalDateTime time;
    /**
     * Message data payload
     */
    private T data;

    public String getCorpKey() {
        return corpKey;
    }

    public void setCorpKey(String corpKey) {
        this.corpKey = corpKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getMsgRoute() {
        return msgRoute;
    }

    public void setMsgRoute(String msgRoute) {
        this.msgRoute = msgRoute;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
