package com.javanext.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javanext.inventory.event.InventoryRejectedEvent;
import com.javanext.order.domain.OrderStatus;
import com.javanext.payment.event.PaymentCompletedEvent;
import com.javanext.payment.event.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderSagaService {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaService.class);

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    // Track processed event IDs for idempotency
    private final Map<UUID, Boolean> processedPaymentEvents = new HashMap<>();
    private final Map<UUID, Boolean> processedInventoryEvents = new HashMap<>();

    public OrderSagaService(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic.payments}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handlePaymentEvent(String message) {
        try {
            logger.info("Received payment event: {}", message);

            // Try to parse as PaymentCompletedEvent
            try {
                PaymentCompletedEvent event = objectMapper.readValue(message, PaymentCompletedEvent.class);

                // Check idempotency
                if (processedPaymentEvents.containsKey(event.getOrderId())) {
                    logger.info("Payment completed event already processed for order: {}", event.getOrderId());
                    return;
                }

                orderService.updateOrderStatus(event.getOrderId(), OrderStatus.PAID);
                processedPaymentEvents.put(event.getOrderId(), true);

                logger.info("Order {} marked as PAID", event.getOrderId());
                return;
            } catch (Exception e) {
                // Not a completed event, try failed event
            }

            // Try to parse as PaymentFailedEvent
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);

            // Check idempotency
            if (processedPaymentEvents.containsKey(event.getOrderId())) {
                logger.info("Payment failed event already processed for order: {}", event.getOrderId());
                return;
            }

            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.FAILED);
            processedPaymentEvents.put(event.getOrderId(), true);

            logger.warn("Order {} marked as FAILED due to payment failure: {}", event.getOrderId(), event.getReason());

        } catch (Exception e) {
            logger.error("Error processing payment event", e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.inventory}", groupId = "${spring.kafka.consumer.group-id}-inventory")
    @Transactional
    public void handleInventoryRejectedEvent(String message) {
        try {
            // Try to parse as InventoryRejectedEvent
            InventoryRejectedEvent event = objectMapper.readValue(message, InventoryRejectedEvent.class);

            // Check idempotency
            if (processedInventoryEvents.containsKey(event.getOrderId())) {
                logger.info("Inventory rejected event already processed for order: {}", event.getOrderId());
                return;
            }

            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED);
            processedInventoryEvents.put(event.getOrderId(), true);

            logger.warn("Order {} marked as CANCELLED due to inventory rejection: {}", event.getOrderId(), event.getReason());

        } catch (Exception e) {
            // Not an inventory rejected event or other error
            logger.debug("Message is not an inventory rejected event or error occurred: {}", e.getMessage());
        }
    }
}
