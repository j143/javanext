package com.javanext.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javanext.order.domain.*;
import com.javanext.order.dto.*;
import com.javanext.order.event.OrderCreatedEvent;
import com.javanext.order.repository.IdempotencyKeyRepository;
import com.javanext.order.repository.OrderRepository;
import com.javanext.order.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        IdempotencyKeyRepository idempotencyKeyRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BulkOrderResponse createBulkOrders(BulkOrderRequest request, String idempotencyKey) {
        // Compute request hash for idempotency
        String requestHash = computeRequestHash(request);

        // Check for existing idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
                .findByCustomerIdAndKey(request.getCustomerId(), idempotencyKey);

        if (existingKey.isPresent()) {
            // Return existing order
            UUID orderId = existingKey.get().getOrderId();
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found for idempotency key"));
            return new BulkOrderResponse(List.of(toOrderResponse(order)));
        }

        // Create new orders
        List<OrderResponse> orderResponses = new ArrayList<>();

        for (OrderCreateRequest orderRequest : request.getOrders()) {
            Order order = createOrder(request.getCustomerId(), orderRequest);
            Order savedOrder = orderRepository.save(order);
            orderResponses.add(toOrderResponse(savedOrder));

            // Create outbox event
            createOutboxEvent(savedOrder);
        }

        // Save idempotency key (for simplicity, storing first order ID)
        if (!orderResponses.isEmpty()) {
            IdempotencyKey key = new IdempotencyKey();
            key.setKey(idempotencyKey);
            key.setCustomerId(request.getCustomerId());
            key.setRequestHash(requestHash);
            key.setOrderId(orderResponses.get(0).getId());
            idempotencyKeyRepository.save(key);
        }

        return new BulkOrderResponse(orderResponses);
    }

    private Order createOrder(UUID customerId, OrderCreateRequest orderRequest) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            order.addItem(item);

            BigDecimal itemTotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        return order;
    }

    private void createOutboxEvent(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.getId(),
                    order.getCustomerId(),
                    order.getTotalAmount(),
                    order.getItems().stream()
                            .map(item -> new com.javanext.order.event.OrderCreatedEvent.OrderItemEvent(
                                    item.getProductId(),
                                    item.getQuantity(),
                                    item.getUnitPrice()
                            ))
                            .collect(Collectors.toList())
            );

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateId(order.getId());
            outboxEvent.setEventType("ORDER_CREATED");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            outboxEvent.setStatus(OutboxEventStatus.NEW);

            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    private String computeRequestHash(BulkOrderRequest request) {
        try {
            // Serialize request to JSON for hashing
            String jsonRequest = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jsonRequest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    private String computeRequestHash(OrderCreateRequest request) {
        try {
            // Serialize request to JSON for hashing
            String jsonRequest = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jsonRequest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String idempotencyKey) {
        // Generate a customer ID (in real app, this would come from auth context)
        UUID customerId = UUID.randomUUID();

        // Compute request hash for idempotency
        String requestHash = computeRequestHash(request);

        // Check for existing idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository
                .findByCustomerIdAndKey(customerId, idempotencyKey);

        if (existingKey.isPresent()) {
            if (!existingKey.get().getRequestHash().equals(requestHash)) {
                throw new RuntimeException("Idempotency key reused with different request data");
            }
            // Return existing order
            return getOrder(existingKey.get().getOrderId());
        }

        // Create order
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(itemRequest.getProductId());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());

            BigDecimal itemTotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            orderItems.add(item);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Store idempotency key
        IdempotencyKey key = new IdempotencyKey();
        key.setCustomerId(customerId);
        key.setKey(idempotencyKey);
        key.setOrderId(savedOrder.getId());
        key.setRequestHash(requestHash);
        idempotencyKeyRepository.save(key);

        // Create outbox event
        createOutboxEvent(savedOrder);

        return toOrderResponse(savedOrder);
    }

    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }
}
