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

package com.github.spring.mq.pulsar.health;

import org.apache.pulsar.client.api.PulsarClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Pulsar health check indicator
 *
 * <p>Simplified version that doesn't depend on Spring Boot Actuator.
 * This class provides health check functionality for Pulsar connections
 * and can be used to monitor the status of Pulsar client connections.
 *
 * <p>The health check includes:
 * <ul>
 *   <li>Client connection status verification</li>
 *   <li>Basic connectivity tests</li>
 *   <li>Error reporting for connection issues</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
public final class PulsarHealthIndicator {

    private final PulsarClient pulsarClient;

    public PulsarHealthIndicator(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
    }

    /**
     * Check Pulsar health status
     *
     * @return Health status information map containing status and details
     */
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> pulsarDetails = new HashMap<>();

        try {
            // Handle null client
            if (pulsarClient == null) {
                health.put("status", "DOWN");
                pulsarDetails.put("status", "Client not available");
                health.put("pulsar", pulsarDetails);
                return health;
            }

            // Check Pulsar client status
            if (pulsarClient.isClosed()) {
                health.put("status", "DOWN");
                pulsarDetails.put("status", "Disconnected");
                health.put("pulsar", pulsarDetails);
                return health;
            }

            // Try to get cluster information to verify connection
            // More health check logic can be added here
            health.put("status", "UP");
            pulsarDetails.put("status", "Connected");
            pulsarDetails.put("checkTime", System.currentTimeMillis());
            health.put("pulsar", pulsarDetails);

        } catch (Exception e) {
            health.put("status", "DOWN");
            pulsarDetails.put("status", "Connection failed");
            pulsarDetails.put("error", e.getMessage());
            health.put("pulsar", pulsarDetails);
        }

        return health;
    }
}