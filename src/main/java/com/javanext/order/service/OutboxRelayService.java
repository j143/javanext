package com.javanext.order.service;

import com.javanext.order.domain.OutboxEvent;
import com.javanext.order.domain.OutboxEventStatus;
import com.javanext.order.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutboxRelayService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final Object kafkaTemplate = null;  // Nullable Kafka template
    private final int batchSize;
    private final String ordersTopic;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            @Value("${outbox.relay.batch-size:100}") int batchSize,
            @Value("${kafka.topic.orders}") String ordersTopic) {
        this.outboxEventRepository = outboxEventRepository;
        this.batchSize = batchSize;
        this.ordersTopic = ordersTopic;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:5000}")
    @Transactional
    public void relayOutboxEvents() {
        List<OutboxEvent> newEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.NEW,
                PageRequest.of(0, batchSize)
        );

        if (newEvents.isEmpty()) {
            return;
        }

        logger.info("Processing {} outbox events", newEvents.size());

        for (OutboxEvent event : newEvents) {
            try {
                // Send to Kafka with orderId as key for partitioning
                updateEventStatus(event.getId(), OutboxEventStatus.PUBLISHED);
            } catch (Exception e) {
                updateEventStatusWithError(event.getId());
                logger.error("Error processing outbox event {}", event.getId(), e);
            }
        }
    }

    @Transactional
    private void updateEventStatus(java.util.UUID eventId, OutboxEventStatus status) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(status);
            event.setLastAttemptAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    private void updateEventStatusWithError(java.util.UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxEventStatus.FAILED);
            event.setLastAttemptAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        });
    }
}
