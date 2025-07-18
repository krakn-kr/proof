package com.interview.microservices.proof.circuitBreaker.service;

// ==============================================
// 6. Service Layer with Fallback Mechanisms
// ==============================================

import com.interview.microservices.proof.circuitBreaker.config.dto.ApiResponse;
import com.interview.microservices.proof.circuitBreaker.config.dto.Order;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private ExternalOrderService externalService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

    @Autowired
    private io.github.resilience4j.timelimiter.TimeLimiter timeLimiter;

    // Method 1: Circuit Breaker with Cache Fallback
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackOrderDetails")
    @TimeLimiter(name = "orderService")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public CompletableFuture<ApiResponse<Order>> getOrderDetailsWithCircuitBreaker(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Attempting to fetch order details for: {}", orderId);
                Order order = externalService.getOrderDetails(orderId);

                // Cache successful response
                cacheService.cacheOrder(orderId, order);
                cacheService.updateServiceHealth("orderService", true);

                return ApiResponse.success(order, "LIVE");
            } catch (Exception e) {
                log.error("External service call failed for order: {}", orderId, e);
                cacheService.updateServiceHealth("orderService", false);
                throw new RuntimeException("Service call failed", e);
            }
        });
    }

    // Fallback method for Circuit Breaker
    public CompletableFuture<ApiResponse<Order>> fallbackOrderDetails(String orderId, Exception ex) {
        log.warn("Fallback triggered for order: {}, reason: {}", orderId, ex.getMessage());

        // Try to get from cache first
        Order cachedOrder = cacheService.getCachedOrder(orderId);
        if (cachedOrder != null) {
            log.info("Returning cached order for: {}", orderId);
            return CompletableFuture.completedFuture(
                    ApiResponse.success(cachedOrder, "CACHE")
            );
        }

        // Return default order as last resort
        Order defaultOrder = Order.defaultOrder(orderId);
        log.info("Returning default order for: {}", orderId);
        return CompletableFuture.completedFuture(
                ApiResponse.fallback(defaultOrder, "Service temporarily unavailable. Default response provided.")
        );
    }

    // Method 2: Manual Fallback with Health Check
    public ApiResponse<Order> getOrderDetailsWithManualFallback(String orderId) {
        try {
            // Check service health first
            if (!cacheService.isServiceHealthy("orderService")) {
                log.warn("Service is marked as unhealthy, using fallback for order: {}", orderId);
                return getFallbackOrderResponse(orderId);
            }

            // Attempt service call
            Order order = externalService.getOrderDetails(orderId);

            // Cache successful response
            cacheService.cacheOrder(orderId, order);
            cacheService.updateServiceHealth("orderService", true);

            return ApiResponse.success(order, "LIVE");

        } catch (Exception e) {
            log.error("Service call failed for order: {}, falling back", orderId, e);
            cacheService.updateServiceHealth("orderService", false);
            return getFallbackOrderResponse(orderId);
        }
    }

    // Method 3: Tiered Fallback Strategy
    public ApiResponse<List<Order>> getOrderHistoryWithTieredFallback(String customerId) {
        // Tier 1: Try live service
        try {
            List<Order> orders = externalService.getOrderHistory(customerId);
            cacheService.cacheFallbackResponse("history:" + customerId, orders);
            return ApiResponse.success(orders, "LIVE");
        } catch (Exception e) {
            log.warn("Live service failed for customer: {}, trying tier 2", customerId, e);
        }

        // Tier 2: Try cache
        try {
            @SuppressWarnings("unchecked")
            List<Order> cachedOrders = (List<Order>) cacheService.getFallbackResponse("history:" + customerId);
            if (cachedOrders != null && !cachedOrders.isEmpty()) {
                return ApiResponse.success(cachedOrders, "CACHE");
            }
        } catch (Exception e) {
            log.warn("Cache retrieval failed for customer: {}, trying tier 3", customerId, e);
        }

        // Tier 3: Return minimal response
        List<Order> defaultOrders = Arrays.asList(
                new Order("UNKNOWN", customerId, "No recent orders", 0.0, "UNAVAILABLE", LocalDateTime.now())
        );
        return ApiResponse.fallback(defaultOrders, "Service temporarily unavailable");
    }

    private ApiResponse<Order> getFallbackOrderResponse(String orderId) {
        // Try cache first
        Order cachedOrder = cacheService.getCachedOrder(orderId);
        if (cachedOrder != null) {
            return ApiResponse.success(cachedOrder, "CACHE");
        }

        // Return default order
        Order defaultOrder = Order.defaultOrder(orderId);
        return ApiResponse.fallback(defaultOrder, "Service temporarily unavailable");
    }
}
