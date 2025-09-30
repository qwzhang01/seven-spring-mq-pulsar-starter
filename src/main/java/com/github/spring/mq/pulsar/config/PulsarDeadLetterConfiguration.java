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

package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar dead letter queue configuration class
 *
 * <p>This configuration class sets up the infrastructure for handling
 * dead letter messages in Pulsar. Dead letter messages are messages
 * that have exceeded the maximum retry attempts and need special handling.
 *
 * <p>The configuration provides a DeadLetterMessageProcessor bean that
 * handles the processing and acknowledgment of dead letter messages.
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarDeadLetterConfiguration {
    /**
     * Dead letter message processor
     *
     * <p>Creates a DeadLetterMessageProcessor bean for handling messages
     * that have been moved to the dead letter queue after exceeding
     * the maximum retry attempts.
     *
     * @return DeadLetterMessageProcessor instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadLetterMessageProcessor deadLetterMessageProcessor() {
        return new DeadLetterMessageProcessor();
    }
}