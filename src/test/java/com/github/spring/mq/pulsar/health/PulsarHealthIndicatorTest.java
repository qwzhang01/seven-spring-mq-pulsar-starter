package com.github.spring.mq.pulsar.health;

import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for Pulsar health indicator
 *
 * @author avinzhang
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pulsar Health Indicator Tests")
class PulsarHealthIndicatorTest {

    @Mock
    private PulsarClient mockPulsarClient;

    private PulsarHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new PulsarHealthIndicator(mockPulsarClient);
    }

    @Test
    @DisplayName("Should report healthy status when client is connected")
    void shouldReportHealthyStatusWhenClientIsConnected() {
        when(mockPulsarClient.isClosed()).thenReturn(false);

        Map<String, Object> healthDetails = healthIndicator.health();

        assertThat(healthDetails).isNotNull();
        assertThat(healthDetails.get("status")).isEqualTo("UP");
        assertThat(healthDetails.get("pulsar")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> pulsarDetails = (Map<String, Object>) healthDetails.get("pulsar");
        assertThat(pulsarDetails.get("status")).isEqualTo("Connected");
    }

    @Test
    @DisplayName("Should report unhealthy status when client is closed")
    void shouldReportUnhealthyStatusWhenClientIsClosed() {
        when(mockPulsarClient.isClosed()).thenReturn(true);

        Map<String, Object> healthDetails = healthIndicator.health();

        assertThat(healthDetails).isNotNull();
        assertThat(healthDetails.get("status")).isEqualTo("DOWN");
        assertThat(healthDetails.get("pulsar")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> pulsarDetails = (Map<String, Object>) healthDetails.get("pulsar");
        assertThat(pulsarDetails.get("status")).isEqualTo("Disconnected");
    }

    @Test
    @DisplayName("Should handle null client gracefully")
    void shouldHandleNullClientGracefully() {
        PulsarHealthIndicator nullClientHealthIndicator = new PulsarHealthIndicator(null);

        Map<String, Object> healthDetails = nullClientHealthIndicator.health();

        assertThat(healthDetails).isNotNull();
        assertThat(healthDetails.get("status")).isEqualTo("DOWN");
        assertThat(healthDetails.get("pulsar")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> pulsarDetails = (Map<String, Object>) healthDetails.get("pulsar");
        assertThat(pulsarDetails.get("status")).isEqualTo("Client not available");
    }

    @Test
    @DisplayName("Should include additional health information")
    void shouldIncludeAdditionalHealthInformation() {
        when(mockPulsarClient.isClosed()).thenReturn(false);

        Map<String, Object> healthDetails = healthIndicator.health();

        assertThat(healthDetails).isNotNull();
        assertThat(healthDetails.get("pulsar")).isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> pulsarDetails = (Map<String, Object>) healthDetails.get("pulsar");
        assertThat(pulsarDetails).containsKey("checkTime");
        assertThat(pulsarDetails.get("checkTime")).isNotNull();
    }
}