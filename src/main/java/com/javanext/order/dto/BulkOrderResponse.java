package com.javanext.order.dto;

import java.util.List;

public class BulkOrderResponse {

    private List<OrderResponse> orders;

    public BulkOrderResponse() {
    }

    public BulkOrderResponse(List<OrderResponse> orders) {
        this.orders = orders;
    }

    // Getters and Setters
    public List<OrderResponse> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderResponse> orders) {
        this.orders = orders;
    }
}
