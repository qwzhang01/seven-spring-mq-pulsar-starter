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

import com.github.spring.mq.pulsar.TestBase;
import com.github.spring.mq.pulsar.core.PulsarTemplate;
import com.github.spring.mq.pulsar.health.PulsarHealthIndicator;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for Pulsar auto-configuration
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.pulsar.enabled=true"
})
@DisplayName("Pulsar Auto Configuration Tests")
class PulsarAutoConfigurationTest extends TestBase {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Should auto-configure PulsarClient bean")
    void shouldAutoConfigurePulsarClient() {
        assertThat(applicationContext.getBean(PulsarClient.class)).isNotNull();
    }

    @Test
    @DisplayName("Should auto-configure PulsarTemplate bean")
    void shouldAutoConfigurePulsarTemplate() {
        assertThat(applicationContext.getBean(PulsarTemplate.class)).isNotNull();
    }

    @Test
    @DisplayName("Should auto-configure PulsarProperties bean")
    void shouldAutoConfigurePulsarProperties() {
        PulsarProperties properties = applicationContext.getBean(PulsarProperties.class);
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should auto-configure health indicator when enabled")
    void shouldAutoConfigureHealthIndicator() {
        assertThat(applicationContext.getBean(PulsarHealthIndicator.class)).isNotNull();
    }

    @Test
    @DisplayName("Should validate producer and consumer configurations")
    void shouldValidateConfigurations() {
        PulsarProperties properties = applicationContext.getBean(PulsarProperties.class);

        // Should not throw exception for valid configuration
        assertThatCode(() -> properties.valid()).doesNotThrowAnyException();
    }
}