package com.ranjan.ordermicroservice.component;

import com.ranjan.ordermicroservice.entity.Outbox;
import com.ranjan.ordermicroservice.enumpackage.OutboxStatus;
import com.ranjan.ordermicroservice.repository.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Define the Kafka topic your downstream Seat Service will listen to
    private static final String KAFKA_TOPIC = "booking-events-topic";

    public OutboxPoller(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * LOOP 1: Outbound Forwarder (Runs every 100 milliseconds)
     * fixedDelay ensures that the 100ms clock only starts ticking AFTER the previous execution finishes,
     * protecting your database from overlapping query thread stampedes.
     */
    @Scheduled(fixedDelay = 100)
    public void pollAndPublishOutboxEvents() {
        // Fetch a small batch of pending outbox events ordered by creation time
        List<Outbox> pendingEvents = outboxRepository.findPendingEventsWithLock(OutboxStatus.PENDING, 10);

        if (pendingEvents.isEmpty()) {
            return; // Exit early if no events are waiting
        }

        // Process each outbox record sequentially
        for (Outbox event : pendingEvents) {
            publishToKafkaAndMarkProcessed(event);
        }
    }

    /**
     * Handles the transactional lifecycle of a single outbox record.
     * Uses aggregateId as the Kafka Message Partition Key to preserve ordering in the cluster.
     */
    @Transactional
    public void publishToKafkaAndMarkProcessed(Outbox event) {
        try {
            // Partition Key = aggregateId (Booking ID). All events for this booking stay sequential.
            String partitionKey = event.getAggregateId();
            String payload = event.getPayload();

            // Send message to Kafka synchronously via get() to ensure broker confirmation before DB update
            kafkaTemplate.send(KAFKA_TOPIC, partitionKey, payload).get();

            // Update outbox state to PROCESSED so it isn't picked up on the next 100ms cycle
            event.setStatus(OutboxStatus.SENT);
            outboxRepository.save(event);

        } catch (Exception e) {
            // Log error cleanly. Leaving the status as PENDING means it will retry automatically in 100ms
            System.err.println("Failed to publish outbox event " + event.getId() + " to Kafka: " + e.getMessage());
        }
    }

    /**
     * LOOP 2: Database Janitor (Runs every night at midnight)
     * Deletes records that have been processed for over 24 hours to prevent table bloat.
     * Cron format: "Second Minute Hour Day Month Day_of_Week"
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeProcessedOutboxEvents() {
        try {
            // Retain records for 24 hours for debugging safety
            LocalDateTime retentionCutoff = LocalDateTime.now().minusDays(1);

            outboxRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.SENT, retentionCutoff);
            System.out.println("Successfully purged processed outbox logs older than 24 hours.");
        } catch (Exception e) {
            System.err.println("Failed to clean up outbox table: " + e.getMessage());
        }
    }
}
