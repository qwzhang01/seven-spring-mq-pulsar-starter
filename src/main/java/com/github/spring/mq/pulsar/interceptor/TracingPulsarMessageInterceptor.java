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
import io.micrometer.tracing.propagation.Propagator;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Tracing interceptor for Pulsar messages that integrates with Micrometer Tracing
 *
 * <p>This interceptor handles distributed tracing context propagation through Pulsar messages.
 * It creates and manages spans for message sending and receiving operations.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatic context propagation via message properties</li>
 *   <li>Span creation for message production and consumption</li>
 *   <li>Integration with Micrometer Tracing API</li>
 * </ul>
 */
public class TracingPulsarMessageInterceptor implements PulsarMessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingPulsarMessageInterceptor.class);

    private final Tracer tracer;
    private final Propagator propagator;

    /**
     * Constructs a new tracing interceptor with the provided tracer and propagator
     *
     * @param tracer the Micrometer tracer instance for span management
     * @param propagator the propagator for context injection and extraction
     */
    public TracingPulsarMessageInterceptor(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /**
     * Injects tracing context into message properties before sending
     *
     * <p>This method is called during message sending to propagate the current
     * trace context through message properties.
     *
     * @param messageBuilder the Pulsar message builder being prepared
     */
    @Override
    public void messageBuilder(TypedMessageBuilder<byte[]> messageBuilder) {
        if (tracer == null) {
            return;
        }
        TraceContext ctx = tracer.currentTraceContext().context();
        if (ctx == null) {
            return;
        }
        propagator.inject(ctx, messageBuilder, (builder, key, value) -> {
            if (builder != null) {
                builder.property(key, value);
            }
        });
    }

    /**
     * Extracts tracing context from received message and starts consumer span
     *
     * <p>This method:
     * <ul>
     *   <li>Extracts trace context from message properties</li>
     *   <li>Creates a new span for message consumption</li>
     *   <li>Sets up tracing context in thread-local storage</li>
     * </ul>
     *
     * @param message the received Pulsar message
     * @return true if tracing context was successfully set up, false otherwise
     */
    @Override
    public boolean beforeReceive(Message<?> message) {
        if (tracer == null) {
            return true;
        }

        try {
            Map<String, String> properties = message.getProperties();
            TraceContext context = tracer.currentTraceContext().context();
            Span consumeSpan = propagator.extract(context, (traceContext, key) -> properties.get(key))
                    .name("consume-pulsar-message")
                    .start();

            Tracer.SpanInScope spanInScope = tracer.withSpan(consumeSpan);
            MsgContext.setSpanInScope(spanInScope);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to create tracing span for message receive", e);
            return true;
        }
    }

    /**
     * Cleans up tracing context after message processing
     *
     * <p>This method:
     * <ul>
     *   <li>Closes the current span scope</li>
     *   <li>Removes thread-local tracing context</li>
     *   <li>Logs completion of message processing</li>
     * </ul>
     *
     * @param message the received Pulsar message
     * @param processedMessage the processed message object
     * @param exception any exception that occurred during processing
     */
    @Override
    public void afterReceive(Message<?> message, Object processedMessage, Exception exception) {
        if (tracer == null) {
            return;
        }
        try (Tracer.SpanInScope spanInScope = MsgContext.getSpanInScope()) {
            logger.debug("Finished processing messageId: {}", message.getMessageId());
        } catch (Exception e) {
            logger.warn("Failed to end tracing span for message receive", e);
        } finally {
            // Clean up all trace context to prevent memory leaks and context pollution
            MsgContext.remove();
        }
    }

    /**
     * Returns the execution order of this interceptor
     *
     * @return the order value (lower numbers execute first)
     */
    @Override
    public int getOrder() {
        return -1000;
    }
}