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