package com.github.spring.mq.pulsar;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * Pulsar Starter Test Suite
 *
 * <p>This is the main test suite that runs all tests for the Pulsar Spring Boot Starter.
 * Run this suite before each release to ensure all functionality works correctly.
 *
 * <p>Test Coverage:
 * <ul>
 *   <li>Configuration and Auto-configuration tests</li>
 *   <li>Core functionality tests (PulsarTemplate, Message Senders)</li>
 *   <li>Annotation processing tests (@PulsarListener, @EnablePulsar)</li>
 *   <li>Exception handling tests</li>
 *   <li>Interceptor tests</li>
 *   <li>Health check tests</li>
 *   <li>Transaction tests</li>
 *   <li>Integration tests</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * mvn test -Dtest=PulsarStarterTestSuite
 * </pre>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Suite
@SuiteDisplayName("Pulsar Spring Boot Starter Test Suite")
@SelectPackages({
        "com.github.spring.mq.pulsar.config",
        "com.github.spring.mq.pulsar.core",
        "com.github.spring.mq.pulsar.annotation",
        "com.github.spring.mq.pulsar.exception",
        "com.github.spring.mq.pulsar.interceptor",
        "com.github.spring.mq.pulsar.health",
        "com.github.spring.mq.pulsar.listener",
        "com.github.spring.mq.pulsar.tracing",
        "com.github.spring.mq.pulsar.transaction",
        "com.github.spring.mq.pulsar.integration"
})
public class PulsarStarterTestSuite {
    // Test suite class - no implementation needed
}