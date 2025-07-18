package com.interview.microservices.proof.circuitBreaker;

// ==============================================
// 10. Main Application Class
// ==============================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class FallbackMechanismDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FallbackMechanismDemoApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
