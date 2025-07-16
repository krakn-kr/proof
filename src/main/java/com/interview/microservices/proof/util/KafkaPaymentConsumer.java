package com.interview.microservices.proof.util;

import com.interview.microservices.proof.config.KafkaConfig;
import com.interview.microservices.proof.model.PaymentEvent;
import com.interview.microservices.proof.service.KafkaPaymentProducer;
import com.interview.microservices.proof.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// 7. Kafka Consumer with DLQ handling
@Component
public class KafkaPaymentConsumer {

    private final Logger logger = LoggerFactory.getLogger(KafkaPaymentConsumer.class);
    private final PaymentService paymentService;
    private final KafkaPaymentProducer producer;

    public KafkaPaymentConsumer(PaymentService paymentService, KafkaPaymentProducer producer) {
        this.paymentService = paymentService;
        this.producer = producer;
    }

    @KafkaListener(topics = KafkaConfig.MAIN_TOPIC, groupId = "payment-service")
    public void handlePaymentEvent(PaymentEvent event,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {

        logger.info("Processing payment event: {} (attempt: {})",
                event.getPaymentId(), event.getRetryCount() + 1);

        try {
            // Process the payment
            paymentService.processPayment(event);
            logger.info("Payment processed successfully: {}", event.getPaymentId());

            // Acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process payment: {} (attempt: {})",
                    event.getPaymentId(), event.getRetryCount() + 1, e);

            // Check if we should retry or send to DLQ
            if (event.getRetryCount() < 3) {
                event.incrementRetryCount();

                // Calculate delay (exponential backoff)
                long delay = (long) Math.pow(2, event.getRetryCount()) * 1000;

                // Send to retry topic with delay (you'd need to implement delay mechanism)
                scheduleRetry(event, delay);

                acknowledgment.acknowledge();
                logger.info("Payment event scheduled for retry after {} ms", delay);

            } else {
                // Max retries reached, send to DLQ
                producer.sendToDlq(event, "Max retries exceeded: " + e.getMessage());
                acknowledgment.acknowledge();
                logger.error("Payment event sent to DLQ after {} attempts: {}",
                        event.getRetryCount() + 1, event.getPaymentId());
            }
        }
    }

    @KafkaListener(topics = KafkaConfig.DLQ_TOPIC, groupId = "payment-dlq-service")
    public void handleDlqMessage(PaymentEvent event, Acknowledgment acknowledgment) {
        logger.error("Processing DLQ message: {} (failed after {} attempts)",
                event.getPaymentId(), event.getRetryCount());

        // Handle DLQ message
        // 1. Log to monitoring system
        // 2. Send alert to administrators
        // 3. Store in database for manual review
        // 4. Send notification to user

        try {
            paymentService.storeFailedPaymentForReview(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Failed to store DLQ message: {}", event.getPaymentId(), e);
            // Don't acknowledge - let it be retried
        }
    }

    private void scheduleRetry(PaymentEvent event, long delay) {
        // Implementation depends on your retry mechanism
        // You could use Spring's @Scheduled, Quartz, or a separate retry topic
        // For simplicity, using a simple delay here

        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    try {
                        producer.sendPaymentEvent(event);
                    } catch (Exception e) {
                        logger.error("Failed to send retry message: {}", event.getPaymentId(), e);
                        producer.sendToDlq(event, "Retry scheduling failed: " + e.getMessage());
                    }
                });
    }
}
