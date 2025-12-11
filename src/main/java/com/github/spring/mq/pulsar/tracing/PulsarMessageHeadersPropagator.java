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

package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.domain.MsgMetaKey;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Utility class for propagating trace context and metadata through Pulsar message headers
 *
 * <p>Provides methods for injecting and extracting:
 * <ul>
 *   <li>Distributed tracing context</li>
 *   <li>Enterprise identifier (corpKey)</li>
 *   <li>Message routing information</li>
 * </ul>
 *
 * <p>Updated to use Micrometer Tracing Propagator API for context propagation
 */
public class PulsarMessageHeadersPropagator {

    private static final Logger logger = LoggerFactory.getLogger(PulsarMessageHeadersPropagator.class);

    /**
     * Injects trace context properties into message builder
     *
     * <p>This method propagates the current trace context by injecting
     * all tracing properties as message headers using the configured
     * {@link Propagator} implementation.
     *
     * @param messageBuilder the Pulsar message builder to inject headers into
     * @param contextMap     the trace context properties to inject
     */
    public static void injectTraceContext(TypedMessageBuilder<?> messageBuilder,
                                          Map<String, String> contextMap) {

        if (contextMap == null || contextMap.isEmpty()) {
            return;
        }
        contextMap.forEach(messageBuilder::property);
    }

    /**
     * Injects enterprise metadata into message builder
     *
     * <p>Adds the following enterprise context information as message properties:
     * <ul>
     *   <li>Enterprise identifier (corpKey)</li>
     *   <li>Application name</li>
     *   <li>Message timestamp</li>
     *   <li>Unique message ID</li>
     * </ul>
     *
     * @param messageBuilder the Pulsar message builder to inject headers into
     * @param corpKey        enterprise identifier
     * @param appName        application name
     * @param time           message timestamp
     * @param msgId          unique message identifier
     */
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
     * Extracts message routing information from properties
     *
     * @param properties message properties
     * @return extracted message route, or null if not found
     */
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
     * Extracts enterprise identifier from properties
     *
     * @param properties message properties
     * @return extracted enterprise identifier, or null if not found
     */
    public static String extractCorp(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }

        try {
            return properties.get(MsgMetaKey.CORP.getCode());
        } catch (Exception e) {
            logger.warn("Failed to extract corp context from message properties", e);
            return null;
        }
    }
}