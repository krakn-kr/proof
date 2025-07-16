package com.interview.microservices.proof.util;

import com.interview.microservices.proof.config.RabbitMQConfig;
import com.interview.microservices.proof.model.PaymentEvent;
import com.interview.microservices.proof.service.PaymentService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

// 4. RabbitMQ Message Consumer with DLQ handling
@Component
public class PaymentEventConsumer {

    private final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;

    public PaymentEventConsumer(PaymentService paymentService, RabbitTemplate rabbitTemplate) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.MAIN_QUEUE)
    public void handlePaymentEvent(PaymentEvent event,
                                   @Header Map<String, Object> headers,
                                   Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        logger.info("Processing payment event: {} (attempt: {})",
                event.getPaymentId(), event.getRetryCount() + 1);

        try {
            // Process the payment
            paymentService.processPayment(event);
            logger.info("Payment processed successfully: {}", event.getPaymentId());

            // Acknowledge the message
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            logger.error("Failed to process payment: {} (attempt: {})",
                    event.getPaymentId(), event.getRetryCount() + 1, e);

            try {
                // Check if we should retry or send to DLQ
                if (event.getRetryCount() < 3) {
                    event.incrementRetryCount();

                    // Requeue with delay (exponential backoff)
                    long delay = (long) Math.pow(2, event.getRetryCount()) * 1000;

                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.MAIN_EXCHANGE,
                            "payment.process",
                            event,
                            message -> {
                                message.getMessageProperties().setDelayLong( delay);
                                return message;
                            }
                    );

                    channel.basicAck(deliveryTag, false);
                    logger.info("Payment event requeued with delay: {} ms", delay);

                } else {
                    // Max retries reached, let it go to DLQ
                    channel.basicNack(deliveryTag, false, false);
                    logger.error("Payment event sent to DLQ after {} attempts: {}",
                            event.getRetryCount() + 1, event.getPaymentId());
                }

            } catch (IOException ioException) {
                logger.error("Error handling message acknowledgment", ioException);
            }
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleDlqMessage(PaymentEvent event) {
        logger.error("Processing DLQ message: {} (failed after {} attempts)",
                event.getPaymentId(), event.getRetryCount());

        // Here you can:
        // 1. Log to monitoring system
        // 2. Send alert to administrators
        // 3. Store in database for manual review
        // 4. Send notification to user

        // Example: Store failed payment for manual review
        paymentService.storeFailedPaymentForReview(event);
    }
}