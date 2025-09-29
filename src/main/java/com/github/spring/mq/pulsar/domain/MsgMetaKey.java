package com.github.spring.mq.pulsar.domain;

/**
 * Message metadata keys
 *
 * <p>Defines standard keys used for message metadata in Pulsar messages.
 * These keys are used for tenant information, tracing, and routing.
 *
 * @author avinzhang
 * @since 1.0.0
 */

public enum MsgMetaKey {
    CORP("corpKey"),
    APP("appName"),

    MSG_ID("msgId"),
    MSG_ROUTE("msgRoute"),

    TRACE_FLAG("X-Trace-Flag"),
    TRACE("X-Trace-Id"),
    SPAN("X-Span-Id"),
    TIME("time");

    private final String code;

    MsgMetaKey(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}