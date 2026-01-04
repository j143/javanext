package com.javanext.order.service;

import com.javanext.inventory.domain.Product;
import com.javanext.inventory.repository.ProductRepository;
import com.javanext.order.domain.Order;
import com.javanext.order.domain.OrderStatus;
import com.javanext.order.dto.*;
import com.javanext.order.repository.IdempotencyKeyRepository;
import com.javanext.order.repository.OrderRepository;
import com.javanext.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProductRepository productRepository;

    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        // Clear data
        outboxEventRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        // Setup test data
        customerId = UUID.randomUUID();

        Product product = new Product();
        product.setSku("TEST-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Test Product");
        product.setQuantity(100);
        product.setReservedQuantity(0);
        product.setPrice(new BigDecimal("99.99"));
        product = productRepository.save(product);
        productId = product.getId();
    }

    @Test
    @Transactional
    void shouldCreateBulkOrders() {
        // Given
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BulkOrderRequest request = createBulkOrderRequest();

        // When
        BulkOrderResponse response = orderService.createBulkOrders(request, idempotencyKey);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrders()).hasSize(1);
        
        OrderResponse orderResponse = response.getOrders().get(0);
        assertThat(orderResponse.getId()).isNotNull();
        assertThat(orderResponse.getCustomerId()).isEqualTo(customerId);
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderResponse.getTotalAmount()).isEqualByComparingTo(new BigDecimal("199.98"));

        // Verify order was saved
        Order savedOrder = orderRepository.findById(orderResponse.getId()).orElse(null);
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getItems()).hasSize(1);

        // Verify outbox event was created
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldHandleIdempotency() {
        // Given
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BulkOrderRequest request = createBulkOrderRequest();

        // When - first request
        BulkOrderResponse response1 = orderService.createBulkOrders(request, idempotencyKey);

        // When - second request with same idempotency key
        BulkOrderRequest request2 = createBulkOrderRequest();
        BulkOrderResponse response2 = orderService.createBulkOrders(request2, idempotencyKey);

        // Then - should return same order
        assertThat(response1.getOrders().get(0).getId())
                .isEqualTo(response2.getOrders().get(0).getId());

        // Verify only one order was created
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldGetOrderById() {
        // Given
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BulkOrderRequest request = createBulkOrderRequest();
        BulkOrderResponse createResponse = orderService.createBulkOrders(request, idempotencyKey);
        UUID orderId = createResponse.getOrders().get(0).getId();

        // When
        OrderResponse response = orderService.getOrder(orderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(orderId);
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void shouldUpdateOrderStatus() {
        // Given
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        BulkOrderRequest request = createBulkOrderRequest();
        BulkOrderResponse createResponse = orderService.createBulkOrders(request, idempotencyKey);
        UUID orderId = createResponse.getOrders().get(0).getId();

        // When
        orderService.updateOrderStatus(orderId, OrderStatus.PAID);

        // Then
        Order updatedOrder = orderRepository.findById(orderId).orElse(null);
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    private BulkOrderRequest createBulkOrderRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("99.99"));

        OrderCreateRequest orderRequest = new OrderCreateRequest();
        orderRequest.setItems(List.of(item));

        BulkOrderRequest bulkRequest = new BulkOrderRequest();
        bulkRequest.setCustomerId(customerId);
        bulkRequest.setOrders(List.of(orderRequest));

        return bulkRequest;
    }
}
