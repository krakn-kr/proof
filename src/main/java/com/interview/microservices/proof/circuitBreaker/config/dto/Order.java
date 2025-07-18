package com.interview.microservices.proof.circuitBreaker.config.dto;

// ==============================================
// 3. DTOs and Models
// ==============================================

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String orderId;
    private String customerId;
    private String productName;
    private double amount;
    private String status;
    private LocalDateTime createdAt;

    // Default fallback order
    public static Order defaultOrder(String orderId) {
        return new Order(
                orderId,
                "UNKNOWN",
                "Service Unavailable",
                0.0,
                "CACHED",
                LocalDateTime.now()
        );
    }
}
