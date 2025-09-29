package com.github.spring.mq.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
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