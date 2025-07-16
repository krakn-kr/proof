package com.interview.microservices.proof.service;

import com.interview.microservices.proof.config.KafkaConfig;
import com.interview.microservices.proof.model.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

// 6. Kafka Producer Service
@Service
public class KafkaPaymentProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final Logger logger = LoggerFactory.getLogger(KafkaPaymentProducer.class);

    public KafkaPaymentProducer(KafkaTemplate<String, PaymentEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentEvent(PaymentEvent event) {
        try {

            kafkaTemplate.send(KafkaConfig.MAIN_TOPIC, event.getPaymentId(), event)
                    .thenAccept(result ->
                            logger.info("Payment event sent successfully: {}", result.getProducerRecord().key()))
                    .exceptionally(ex -> {
                        logger.error("Failed to send payment event: {}", event.getPaymentId(), ex);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error sending payment event: {}", event.getPaymentId(), e);
            throw new RuntimeException("Failed to send payment event", e);
        }
    }

    public void sendToDlq(PaymentEvent event, String reason) {
        try {
            // Add metadata about the failure
            event.setRetryCount(-1); // Mark as DLQ message

            kafkaTemplate.send(KafkaConfig.DLQ_TOPIC, event.getPaymentId(), event)
                    .thenAccept(
                            result -> logger.info("Payment event sent to DLQ: {} (reason: {})",
                                    event.getPaymentId(), reason)

                    ).exceptionally( (failure )-> {logger.error("Failed to send payment event to DLQ: {}",
                            event.getPaymentId(), failure);return null;});
        } catch (Exception e) {
            logger.error("Error sending payment event to DLQ: {}", event.getPaymentId(), e);
        }
    }
}

