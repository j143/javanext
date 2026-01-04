package com.javanext.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public class BulkOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotEmpty(message = "Orders list cannot be empty")
    @Size(max = 100, message = "Maximum 100 orders can be created at once")
    @Valid
    private List<OrderCreateRequest> orders;

    // Getters and Setters
    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<OrderCreateRequest> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderCreateRequest> orders) {
        this.orders = orders;
    }
}
