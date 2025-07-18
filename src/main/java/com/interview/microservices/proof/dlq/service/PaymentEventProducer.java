package com.interview.microservices.proof.dlq.service;

import com.interview.microservices.proof.dlq.config.RabbitMQConfig;
import com.interview.microservices.proof.dlq.model.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

// 3. RabbitMQ Message Producer
@Service
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final Logger logger = LoggerFactory.getLogger(PaymentEventProducer.class);

    public PaymentEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPaymentEvent(PaymentEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MAIN_EXCHANGE,
                    "payment.process",
                    event
            );
            logger.info("Payment event sent: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Failed to send payment event: {}", event.getPaymentId(), e);
            throw new RuntimeException("Failed to send payment event", e);
        }
    }
}
