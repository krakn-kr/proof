package com.interview.microservices.proof.dlq.service;


import com.interview.microservices.proof.dlq.model.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// 8. Payment Service (Business Logic)
@Service
public class PaymentService {

    private final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public void processPayment(PaymentEvent event) {
        // Simulate processing logic
        logger.info("Processing payment: {} for user: {} amount: {}",
                event.getPaymentId(), event.getUserId(), event.getAmount());

        // Simulate potential failures
        if (event.getPaymentId().contains("fail")) {
            throw new RuntimeException("Payment processing failed");
        }

        // Simulate database save, API calls, etc.
        try {
            Thread.sleep(100); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Payment processed successfully: {}", event.getPaymentId());
    }

    public void storeFailedPaymentForReview(PaymentEvent event) {
        // Store failed payment in database for manual review
        logger.info("Storing failed payment for manual review: {}", event.getPaymentId());

        // Implementation would involve saving to database
        // with failure details, timestamps, etc.
    }
}
