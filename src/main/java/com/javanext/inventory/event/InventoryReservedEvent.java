package com.javanext.inventory.event;

import java.util.UUID;

public class InventoryReservedEvent {

    private UUID orderId;
    private UUID customerId;

    public InventoryReservedEvent() {
    }

    public InventoryReservedEvent(UUID orderId, UUID customerId) {
        this.orderId = orderId;
        this.customerId = customerId;
    }

    // Getters and Setters
    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }
}
