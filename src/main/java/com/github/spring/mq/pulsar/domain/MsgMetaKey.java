package com.github.spring.mq.pulsar.domain;

/**
 * 消息元信息 key
 *
 * @author avinzhang
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