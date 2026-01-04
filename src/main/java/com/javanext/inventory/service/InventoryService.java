package com.javanext.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javanext.inventory.domain.Product;
import com.javanext.inventory.event.InventoryRejectedEvent;
import com.javanext.inventory.event.InventoryReservedEvent;
import com.javanext.inventory.repository.ProductRepository;
import com.javanext.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String inventoryTopic;

    // Track processed event IDs for idempotency
    // NOTE: In-memory tracking is acceptable for demo/first-pass implementation
    // For production, use database-backed idempotency tracking for persistence and thread-safety
    private final Map<UUID, Boolean> processedEvents = new HashMap<>();

    public InventoryService(
            ProductRepository productRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${kafka.topic.inventory}") String inventoryTopic) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.inventoryTopic = inventoryTopic;
    }

    @KafkaListener(topics = "${kafka.topic.orders}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCreated(String message) {
        try {
            logger.info("Received order created event: {}", message);

            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            // Check idempotency
            if (processedEvents.containsKey(event.getOrderId())) {
                logger.info("Event already processed for order: {}", event.getOrderId());
                return;
            }

            // Try to reserve inventory for all items
            boolean canReserveAll = true;
            String rejectionReason = null;

            for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                Product product = productRepository.findById(item.getProductId()).orElse(null);

                if (product == null) {
                    canReserveAll = false;
                    rejectionReason = "Product not found: " + item.getProductId();
                    break;
                }

                if (!product.canReserve(item.getQuantity())) {
                    canReserveAll = false;
                    rejectionReason = "Insufficient stock for product: " + product.getSku();
                    break;
                }
            }

            if (canReserveAll) {
                // Reserve all items
                for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
                    Product product = productRepository.findById(item.getProductId()).get();
                    product.reserve(item.getQuantity());
                    productRepository.save(product);
                }

                // Publish inventory reserved event
                InventoryReservedEvent reservedEvent = new InventoryReservedEvent(
                        event.getOrderId(),
                        event.getCustomerId()
                );
                String payload = objectMapper.writeValueAsString(reservedEvent);
                kafkaTemplate.send(inventoryTopic, event.getOrderId().toString(), payload);

                logger.info("Inventory reserved for order: {}", event.getOrderId());
            } else {
                // Publish inventory rejected event
                InventoryRejectedEvent rejectedEvent = new InventoryRejectedEvent(
                        event.getOrderId(),
                        rejectionReason
                );
                String payload = objectMapper.writeValueAsString(rejectedEvent);
                kafkaTemplate.send(inventoryTopic, event.getOrderId().toString(), payload);

                logger.warn("Inventory rejected for order: {} - Reason: {}", event.getOrderId(), rejectionReason);
            }

            // Mark as processed
            processedEvents.put(event.getOrderId(), true);

        } catch (Exception e) {
            logger.error("Error processing order created event", e);
            throw new RuntimeException("Failed to process inventory reservation", e);
        }
    }
}
