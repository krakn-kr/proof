package com.interview.microservices.proof.circuitBreaker.controller;

// ==============================================
// 7. REST Controller
// ==============================================

import com.interview.microservices.proof.circuitBreaker.config.dto.ApiResponse;
import com.interview.microservices.proof.circuitBreaker.config.dto.Order;
import com.interview.microservices.proof.circuitBreaker.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrderDetails(@PathVariable String orderId) {
        try {
            // Using Circuit Breaker approach
            CompletableFuture<ApiResponse<Order>> future = orderService.getOrderDetailsWithCircuitBreaker(orderId);
            ApiResponse<Order> response = future.get(5, TimeUnit.SECONDS);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error getting order details for: {}", orderId, e);
            Order defaultOrder = Order.defaultOrder(orderId);
            ApiResponse<Order> fallbackResponse = ApiResponse.fallback(
                    defaultOrder,
                    "Service error: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallbackResponse);
        }
    }

    @GetMapping("/{orderId}/manual-fallback")
    public ResponseEntity<ApiResponse<Order>> getOrderDetailsManual(@PathVariable String orderId) {
        ApiResponse<Order> response = orderService.getOrderDetailsWithManualFallback(orderId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/history/{customerId}")
    public ResponseEntity<ApiResponse<List<Order>>> getOrderHistory(@PathVariable String customerId) {
        ApiResponse<List<Order>> response = orderService.getOrderHistoryWithTieredFallback(customerId);
        HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;
        return ResponseEntity.status(status).body(response);
    }
}
