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

package com.github.spring.mq.pulsar.annotation;

import java.lang.annotation.*;

/**
 * Pulsar listener annotation
 *
 * <p>This annotation is used to mark methods as Pulsar message listeners.
 * Methods annotated with @PulsarListener will automatically consume messages
 * from the specified topic.
 *
 * <p>Example usage:
 * <pre>
 * &#64;PulsarListener(topic = "my-topic", subscription = "my-subscription")
 * public void handleMessage(String message) {
 *     // Process the message
 * }
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PulsarListener {

    /**
     * Topic name
     */
    String topic() default "";

    /**
     * Message routing
     * <p>
     * Annotates message handler methods, can map handler methods according to
     * the field specified by routeKey in the message body
     */
    String msgRoute() default "";

    /**
     * Message routing key, default is msgRoute
     * <p>
     * Under the same topic, there may be multiple subdivided business types.
     * Use routeKey to distinguish, when parsing, find the corresponding message
     * handler according to the field specified by routeKey
     */
    String routeKey() default "msgRoute";

    /**
     * Data key, default is data
     * <p>
     * Message entity has a unified wrapper class, the wrapper class contains
     * some meta information of the message. The message itself will be in the
     * dataKey field, only need to parse the field specified by dataKey when parsing
     */
    String dataKey() default "data";

    /**
     * Consumer name
     * Identifies consumer instances, used for log identification in distributed
     * systems, does not affect message consumption
     */
    String consumerName() default "";

    /**
     * Message type
     */
    Class<?> messageType() default String.class;
}