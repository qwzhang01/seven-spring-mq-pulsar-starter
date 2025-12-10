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
import com.github.spring.mq.pulsar.domain.MsgMetaKey;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Abstract base class for multi-tenant message interceptors
 *
 * <p>Provides common infrastructure for tenant context propagation in Pulsar messages.
 *
 * <p>Features:
 * <ul>
 *   <li>Tenant context propagation</li>
 *   <li>Request tracing support</li>
 *   <li>Thread-local context management</li>
 * </ul>
 *
 * <p>Implementations should provide concrete implementations for:
 * <ul>
 *   <li>{@link #buildSendContext()} - setup tenant context before sending</li>
 *   <li>{@link #buildReceiveContext(String)} - setup tenant context upon reception</li>
 * </ul>
 */
public abstract class MetaMessageInterceptor implements PulsarMessageInterceptor {
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private final static Logger logger = LoggerFactory.getLogger(MetaMessageInterceptor.class);

    protected MetaMessageInterceptor() {
    }

    @Override
    public void messageBuilder(TypedMessageBuilder<byte[]> messageBuilder) {

        String corpKey = MsgContext.getCorpKey();
        if (corpKey != null && !corpKey.isEmpty()) {
            messageBuilder.property(MsgMetaKey.CORP.getCode(), corpKey);
        }

        String msgRoute = MsgContext.getMsgRoute();
        if (msgRoute != null && !msgRoute.isEmpty()) {
            messageBuilder.property(MsgMetaKey.MSG_ROUTE.getCode(), msgRoute);
        }

        LocalDateTime time = MsgContext.getTime();
        if (time != null) {
            messageBuilder.property(MsgMetaKey.TIME.getCode(), time.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        }
    }

    /**
     * Builds send context and returns original message
     *
     * <p>This implementation:
     * <ul>
     *   <li>Invokes {@link #buildSendContext()} to setup tenant context</li>
     *   <li>Returns the original message unchanged</li>
     * </ul>
     *
     * @param topic   the destination topic
     * @param message the message payload
     * @return the original message payload
     */
    @Override
    public Object beforeSend(String topic, Object message) {
        buildSendContext();
        return message;
    }

    /**
     * Extracts tenant context and sets up receive context
     *
     * <p>This implementation:
     * <ul>
     *   <li>Extracts enterprise identifier (corpKey) from message properties</li>
     *   <li>Invokes {@link #buildReceiveContext(String)} with extracted corpKey</li>
     * </ul>
     *
     * @param message the received message
     * @return true if context was successfully set up, false otherwise
     */
    @Override
    public boolean beforeReceive(Message<?> message) {
        Map<String, String> properties = message.getProperties();
        String corpKey = properties.get(MsgMetaKey.CORP.getCode());
        String time = properties.get(MsgMetaKey.TIME.getCode());

        MsgContext.setCorpKey(corpKey);
        MsgContext.setTime(LocalDateTime.parse(time, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));

        return buildReceiveContext(corpKey);
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