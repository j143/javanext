package com.javanext.order.controller;

import com.javanext.order.dto.BulkOrderRequest;
import com.javanext.order.dto.BulkOrderResponse;
import com.javanext.order.dto.OrderResponse;
import com.javanext.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create bulk orders", description = "Create multiple orders in a single request with idempotency support")
    public ResponseEntity<BulkOrderResponse> createBulkOrders(
            @Valid @RequestBody BulkOrderRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        BulkOrderResponse response = orderService.createBulkOrders(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        OrderResponse order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }
}
