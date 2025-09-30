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
