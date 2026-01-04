package com.javanext.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCompletedEvent {

    private UUID orderId;
    private UUID paymentId;
    private BigDecimal amount;
    private String providerRef;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(UUID orderId, UUID paymentId, BigDecimal amount, String providerRef) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.amount = amount;
        this.providerRef = providerRef;
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public void setProviderRef(String providerRef) {
        this.providerRef = providerRef;
    }
}
