package com.interview.microservices.proof.circuitBreaker.service;

// ==============================================
// 4. Cache Service
// ==============================================

import com.interview.microservices.proof.circuitBreaker.config.dto.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_CACHE_PREFIX = "order:";
    private static final String FALLBACK_CACHE_PREFIX = "fallback:";

    public void cacheOrder(String orderId, Order order) {
        try {
            String key = ORDER_CACHE_PREFIX + orderId;
            redisTemplate.opsForValue().set(key, order, Duration.ofHours(1));
            log.info("Cached order: {}", orderId);
        } catch (Exception e) {
            log.error("Failed to cache order: {}", orderId, e);
        }
    }

    public Order getCachedOrder(String orderId) {
        try {
            String key = ORDER_CACHE_PREFIX + orderId;
            return (Order) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to retrieve cached order: {}", orderId, e);
            return null;
        }
    }

    public void cacheFallbackResponse(String key, Object response) {
        try {
            String fallbackKey = FALLBACK_CACHE_PREFIX + key;
            redisTemplate.opsForValue().set(fallbackKey, response, Duration.ofHours(24));
            log.info("Cached fallback response for key: {}", key);
        } catch (Exception e) {
            log.error("Failed to cache fallback response: {}", key, e);
        }
    }

    public Object getFallbackResponse(String key) {
        try {
            String fallbackKey = FALLBACK_CACHE_PREFIX + key;
            return redisTemplate.opsForValue().get(fallbackKey);
        } catch (Exception e) {
            log.error("Failed to retrieve fallback response: {}", key, e);
            return null;
        }
    }

    public boolean isServiceHealthy(String serviceName) {
        try {
            String healthKey = "health:" + serviceName;
            String status = (String) redisTemplate.opsForValue().get(healthKey);
            return "UP".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    public void updateServiceHealth(String serviceName, boolean isHealthy) {
        try {
            String healthKey = "health:" + serviceName;
            redisTemplate.opsForValue().set(healthKey, isHealthy ? "UP" : "DOWN", Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("Failed to update service health: {}", serviceName, e);
        }
    }
}

