package com.javanext.payment.event;

import java.util.UUID;

public class PaymentFailedEvent {

    private UUID orderId;
    private UUID paymentId;
    private String reason;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(UUID orderId, UUID paymentId, String reason) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
