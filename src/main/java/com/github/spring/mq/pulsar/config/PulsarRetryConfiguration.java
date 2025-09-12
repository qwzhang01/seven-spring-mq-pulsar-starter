package com.github.spring.mq.pulsar.config;

import com.github.spring.mq.pulsar.retry.PulsarRetryTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pulsar 重试配置类
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(PulsarRetryConfiguration.PulsarRetryProperties.class)
public class PulsarRetryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PulsarRetryTemplate pulsarRetryTemplate(PulsarRetryProperties retryProperties) {
        return new PulsarRetryTemplate(
                retryProperties.getMaxRetries(),
                retryProperties.getInitialDelay(),
                retryProperties.getMultiplier(),
                retryProperties.getMaxDelay(),
                retryProperties.isUseRandomDelay()
        );
    }

    /**
     * 重试配置属性
     */
    @ConfigurationProperties(prefix = "spring.pulsar.retry")
    public static class PulsarRetryProperties {
        private int maxRetries = 3;
        private long initialDelay = 1000;
        private double multiplier = 2.0;
        private long maxDelay = 30000;
        private boolean useRandomDelay = true;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public void setInitialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(long maxDelay) {
            this.maxDelay = maxDelay;
        }

        public boolean isUseRandomDelay() {
            return useRandomDelay;
        }

        public void setUseRandomDelay(boolean useRandomDelay) {
            this.useRandomDelay = useRandomDelay;
        }
    }
}