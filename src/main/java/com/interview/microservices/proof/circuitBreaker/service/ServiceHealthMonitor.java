package com.interview.microservices.proof.circuitBreaker.service;

// ==============================================
// 9. Health Check and Monitoring
// ==============================================

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
public class ServiceHealthMonitor {

    @Autowired
    private CacheService cacheService;

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void monitorServiceHealth() {
        // This could ping actual services or check metrics
        boolean isHealthy = checkExternalServiceHealth();
        cacheService.updateServiceHealth("orderService", isHealthy);

        if (!isHealthy) {
            log.warn("External order service is unhealthy");
        }
    }

    private boolean checkExternalServiceHealth() {
        // Implement actual health check logic
        // For demo, we'll use a simple random check
        return new Random().nextBoolean();
    }
}
