package com.maze.nlpcustomerffeedbackanalyzer.config;

import com.maze.nlpcustomerffeedbackanalyzer.client.NlpServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("NlpServiceHealthIndicator")
@RequiredArgsConstructor
@Slf4j
public class NlpServiceHealthIndicator implements HealthIndicator {

    private final NlpServiceClient nlpServiceClient;

    @Override
    public Health health() {
        try {
            boolean isHealthy = nlpServiceClient.isHealthy();
            if (isHealthy) {
                return Health.up()
                        .withDetail("service", "NLP Flask Microservice")
                        .withDetail("status", "Connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "NLP Flask Microservice")
                        .withDetail("status", "Unable to connect")
                        .withDetail("message", "NLP service health check failed")
                        .build();
            }
        } catch (Exception e) {
            log.warn("NLP service health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "NLP Flask Microservice")
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}

