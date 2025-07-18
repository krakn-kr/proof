package com.interview.microservices.proof.dlq.controller;

import com.interview.microservices.proof.dlq.model.PaymentEvent;
import com.interview.microservices.proof.dlq.service.KafkaPaymentProducer;
import com.interview.microservices.proof.dlq.service.PaymentEventProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 9. Controller for Testing
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentEventProducer rabbitProducer;
    private final KafkaPaymentProducer kafkaProducer;

    public PaymentController(PaymentEventProducer rabbitProducer,
                             KafkaPaymentProducer kafkaProducer) {
        this.rabbitProducer = rabbitProducer;
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/rabbit")
    public ResponseEntity<String> sendRabbitPayment(@RequestBody PaymentEvent event) {
        try {
            rabbitProducer.sendPaymentEvent(event);
            return ResponseEntity.ok("Payment event sent via RabbitMQ");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send payment event");
        }
    }

    @PostMapping("/kafka")
    public ResponseEntity<String> sendKafkaPayment(@RequestBody PaymentEvent event) {
        try {
            kafkaProducer.sendPaymentEvent(event);
            return ResponseEntity.ok("Payment event sent via Kafka");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send payment event");
        }
    }
}