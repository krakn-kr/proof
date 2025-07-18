package com.interview.microservices.proof.circuitBreaker.service;

// ==============================================
// 5. External Service Client (Simulated)
// ==============================================

import com.interview.microservices.proof.circuitBreaker.config.dto.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class ExternalOrderService {

    @Value("${external.service.failure-rate:0.3}")
    private double failureRate;

    @Value("${external.service.delay:1000}")
    private long responseDelay;

    private final Random random = new Random();

    public Order getOrderDetails(String orderId) throws Exception {
        // Simulate network delay
        Thread.sleep(responseDelay);

        // Simulate service failures
        if (random.nextDouble() < failureRate) {
            throw new RuntimeException("External service is unavailable");
        }

        // Simulate successful response
        return new Order(
                orderId,
                "CUST_" + random.nextInt(1000),
                "Product_" + random.nextInt(100),
                random.nextDouble() * 1000,
                "ACTIVE",
                LocalDateTime.now()
        );
    }

    public List<Order> getOrderHistory(String customerId) throws Exception {
        Thread.sleep(responseDelay);

        if (random.nextDouble() < failureRate) {
            throw new RuntimeException("External service is unavailable");
        }

        return Arrays.asList(
                new Order("ORD_001", customerId, "Product A", 299.99, "COMPLETED", LocalDateTime.now().minusDays(1)),
                new Order("ORD_002", customerId, "Product B", 149.99, "SHIPPED", LocalDateTime.now().minusDays(2))
        );
    }
}
