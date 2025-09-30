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
import com.github.spring.mq.pulsar.domain.MsgDomain;
import com.github.spring.mq.pulsar.domain.MsgMetaKey;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Default multi-tenant interceptor
 *
 * <p>This interceptor handles multi-tenant metadata through message properties.
 * Tenant information and other metadata are stored in the message properties
 * rather than in the message body.
 *
 * <p>When sending messages, tenant information is placed in message properties
 * (implementation details in PulsarTemplate). When receiving messages, metadata
 * is parsed and tenant information is placed in thread-local variables.
 *
 * <p>This abstract interceptor provides multi-tenancy support for Pulsar messages.
 * It automatically wraps outgoing messages with tenant context information and
 * extracts tenant context from incoming messages.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic message wrapping with {@link MsgDomain}</li>
 *   <li>Tenant context propagation</li>
 *   <li>Request tracing support</li>
 *   <li>Thread-local context management</li>
 * </ul>
 *
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #buildSendContext()} - Set up context before sending</li>
 *   <li>{@link #buildReceiveContext(String)} - Handle tenant switching on receive</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public abstract class DefaultMultipleTenantInterceptor implements PulsarMessageInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(DefaultMultipleTenantInterceptor.class);
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public Object beforeSend(String topic, Object message) {
        buildSendContext();
        MsgContext.setMultiTenant(true);
        return message;
    }

    @Override
    public void afterSend(String topic, Object message, MessageId messageId, Throwable exception) {
        MsgContext.remove();
    }

    @Override
    public boolean beforeReceive(Message<?> message) {
        Map<String, String> properties = message.getProperties();
        String corpKey = properties.get(MsgMetaKey.CORP.getCode());
        String appName = properties.get(MsgMetaKey.APP.getCode());
        String time = properties.get(MsgMetaKey.TIME.getCode());

        MsgContext.setCorpKey(corpKey);
        MsgContext.setAppName(appName);
        MsgContext.setTime(LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));

        return buildReceiveContext(corpKey);
    }

    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        MsgContext.remove();
    }

    @Override
    public int getOrder() {
        // Highest priority to ensure proper context setup
        return 10;
    }

    /**
     * Build request context before sending message
     *
     * <p>Implementations should set up the necessary context information
     * in {@link MsgContext} before message sending.
     */
    public abstract void buildSendContext();

    /**
     * Handle multi-tenant context switching after receiving message
     *
     * <p>Implementations should handle tenant switching based on the
     * corporation key extracted from the message.
     *
     * @param corpKey Corporation key for tenant identification
     * @return true if context switching is successful, false otherwise
     */
    public abstract boolean buildReceiveContext(String corpKey);
}
