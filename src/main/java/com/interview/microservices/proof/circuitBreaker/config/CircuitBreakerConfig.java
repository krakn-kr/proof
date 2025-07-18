package com.interview.microservices.proof.circuitBreaker.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

// Circuit Breaker Configuration
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public CircuitBreaker orderServiceCircuitBreaker() {
        return CircuitBreaker.ofDefaults("orderService");
    }

    @Bean
    public TimeLimiter orderServiceTimeLimiter() {
        return TimeLimiter.of(Duration.ofSeconds(3));
    }
}

