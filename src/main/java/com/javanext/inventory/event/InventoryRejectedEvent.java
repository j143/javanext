package com.javanext.inventory.event;

import java.util.UUID;

public class InventoryRejectedEvent {

    private UUID orderId;
    private String reason;

    public InventoryRejectedEvent() {
    }

    public InventoryRejectedEvent(UUID orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
