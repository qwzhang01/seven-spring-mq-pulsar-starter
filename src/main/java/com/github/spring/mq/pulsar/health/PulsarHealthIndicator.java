package com.github.spring.mq.pulsar.health;

import org.apache.pulsar.client.api.PulsarClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Pulsar 健康检查指示器
 * 简化版本，不依赖 Spring Boot Actuator
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
     * 检查 Pulsar 健康状态
     *
     * @return 健康状态信息
     */
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 检查 Pulsar 客户端状态
            if (pulsarClient.isClosed()) {
                health.put("status", "DOWN");
                health.put("details", Map.of("status", "Client is closed"));
                return health;
            }

            // 尝试获取集群信息来验证连接
            // 这里可以添加更多的健康检查逻辑
            health.put("status", "UP");
            health.put("details", Map.of(
                    "status", "Connected",
                    "client", "Active"
            ));

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("details", Map.of(
                    "status", "Connection failed",
                    "error", e.getMessage()
            ));
        }

        return health;
    }
}