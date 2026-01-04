package com.javanext.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javanext.inventory.event.InventoryReservedEvent;
import com.javanext.payment.domain.Payment;
import com.javanext.payment.domain.PaymentStatus;
import com.javanext.payment.event.PaymentCompletedEvent;
import com.javanext.payment.event.PaymentFailedEvent;
import com.javanext.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
private final Object kafkaTemplate = null;  // Nullable Kafka template
    private final ObjectMapper objectMapper;
    private final String paymentsTopic;
    private final Random random = new Random();

    // Track processed event IDs for idempotency
    // NOTE: In-memory tracking is acceptable for demo/first-pass implementation
    // For production, use database-backed idempotency tracking for persistence and thread-safety
    private final Map<UUID, Boolean> processedEvents = new HashMap<>();

    public PaymentService(
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper,
            @Value("${kafka.topic.payments}") String paymentsTopic) {
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.paymentsTopic = paymentsTopic;
    }

    @KafkaListener(topics = "${kafka.topic.inventory}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleInventoryReserved(String message) {
        try {
            logger.info("Received inventory reserved event: {}", message);

            InventoryReservedEvent event = objectMapper.readValue(message, InventoryReservedEvent.class);

            // Check idempotency
            if (processedEvents.containsKey(event.getOrderId())) {
                logger.info("Event already processed for order: {}", event.getOrderId());
                return;
            }

            // Check if payment already exists
            if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
                logger.info("Payment already exists for order: {}", event.getOrderId());
                processedEvents.put(event.getOrderId(), true);
                return;
            }

            // Simulate payment processing with delay
            // TODO: Replace Thread.sleep with reactive/async processing for better throughput
            Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds delay

            // Simulate random payment failure (20% chance)
            boolean paymentSucceeded = random.nextDouble() > 0.2;

            Payment payment = new Payment();
            payment.setOrderId(event.getOrderId());
            // TODO: Get actual order amount from event payload or order lookup
            payment.setAmount(java.math.BigDecimal.ZERO);
            payment.setProviderRef(UUID.randomUUID().toString());

            if (paymentSucceeded) {
                payment.setStatus(PaymentStatus.COMPLETED);
                Payment savedPayment = paymentRepository.save(payment);

                // Publish payment completed event
                PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                        event.getOrderId(),
                        savedPayment.getId(),
                        savedPayment.getAmount(),
                        savedPayment.getProviderRef()
                );
                String payload = objectMapper.writeValueAsString(completedEvent);

                logger.info("Payment completed for order: {}", event.getOrderId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                Payment savedPayment = paymentRepository.save(payment);

                // Publish payment failed event
                PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                        event.getOrderId(),
                        savedPayment.getId(),
                        "Payment gateway declined transaction"
                );
                String payload = objectMapper.writeValueAsString(failedEvent);

                logger.warn("Payment failed for order: {}", event.getOrderId());
            }

            // Mark as processed
            processedEvents.put(event.getOrderId(), true);

        } catch (Exception e) {
            logger.error("Error processing inventory reserved event", e);
            throw new RuntimeException("Failed to process payment", e);
        }
    }
}
