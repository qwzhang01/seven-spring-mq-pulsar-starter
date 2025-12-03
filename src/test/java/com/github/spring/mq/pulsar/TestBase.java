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

package com.github.spring.mq.pulsar;

import com.github.spring.mq.pulsar.config.PulsarAutoConfiguration;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base test class for Pulsar integration tests
 *
 * <p>This class provides common test infrastructure including:
 * <ul>
 *   <li>Testcontainers Pulsar setup</li>
 *   <li>Common test configuration</li>
 *   <li>Shared test utilities</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class TestBase {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(PulsarAutoConfiguration.class)
    static class TestConfiguration {
    }

    @Container
    protected static final PulsarContainer PULSAR_CONTAINER = new PulsarContainer(
            DockerImageName.parse("apachepulsar/pulsar:3.2.4")
    );

    protected static PulsarClient testPulsarClient;

    @BeforeAll
    static void setUpTestContainer() throws PulsarClientException {
        PULSAR_CONTAINER.start();

        // Set system properties for test configuration
        System.setProperty("spring.pulsar.service-url", PULSAR_CONTAINER.getPulsarBrokerUrl());

        // Create test Pulsar client
        testPulsarClient = PulsarClient.builder()
                .serviceUrl(PULSAR_CONTAINER.getPulsarBrokerUrl())
                .build();
    }

    @AfterAll
    static void tearDownTestContainer() throws PulsarClientException {
        if (testPulsarClient != null) {
            testPulsarClient.close();
        }
        PULSAR_CONTAINER.stop();
    }

    /**
     * Get the Pulsar service URL for tests
     */
    protected String getPulsarServiceUrl() {
        return PULSAR_CONTAINER.getPulsarBrokerUrl();
    }

    /**
     * Get the test Pulsar client
     */
    protected PulsarClient getTestPulsarClient() {
        return testPulsarClient;
    }
}