package com.interview.microservices.proof.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;

// 2. Payment Event Model
public class PaymentEvent {
    private String paymentId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
    private int retryCount = 0;

    // Constructors
    public PaymentEvent() {}

    public PaymentEvent(String paymentId, String userId, BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public void incrementRetryCount() { this.retryCount++; }
}