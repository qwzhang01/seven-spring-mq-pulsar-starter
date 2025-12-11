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

import com.github.spring.mq.pulsar.interceptor.DefaultMetaMessageInterceptor;
import com.github.spring.mq.pulsar.interceptor.MetaMessageInterceptor;
import com.github.spring.mq.pulsar.interceptor.PulsarMessageInterceptor;
import org.apache.pulsar.client.api.PulsarClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

/**
 * Pulsar interceptor configuration class
 *
 * <p>This configuration class sets up the interceptor infrastructure for Pulsar
 * message processing. It automatically discovers all PulsarMessageInterceptor
 * beans and registers them in a sorted registry based on their priority.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic interceptor discovery and registration</li>
 *   <li>Priority-based interceptor ordering</li>
 *   <li>Conditional configuration based on PulsarClient presence</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnBean(PulsarClient.class)
public class PulsarInterceptorConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PulsarInterceptorRegistry pulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        return new PulsarInterceptorRegistry(interceptors);
    }

    @Bean
    @ConditionalOnMissingBean
    public MetaMessageInterceptor defaultMetaMessageInterceptor() {
        return new DefaultMetaMessageInterceptor();
    }

    /**
     * Interceptor registry
     *
     * <p>A record that holds all registered PulsarMessageInterceptor instances
     * sorted by their priority order. Lower order values indicate higher priority.
     *
     * @param interceptors List of interceptors sorted by priority
     */
    public record PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
        public PulsarInterceptorRegistry(List<PulsarMessageInterceptor> interceptors) {
            this.interceptors = interceptors;
            // Sort by priority order
            this.interceptors.sort(Comparator.comparingInt(PulsarMessageInterceptor::getOrder));
        }
    }
}