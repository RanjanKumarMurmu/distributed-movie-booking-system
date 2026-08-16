package com.ranjan.payment_microservice.component;

import com.ranjan.payment_microservice.entity.Outbox;
import com.ranjan.payment_microservice.enumpackage.OutboxStatus;
import com.ranjan.payment_microservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String KAFKA_TOPIC = "payment-response-topic";

    @Scheduled(fixedDelay = 100)
    public void pollAndPublishOutboxEvents() {
        List<Outbox> pendingEvents = outboxRepository.findPendingEventsWithLock(OutboxStatus.PENDING, 10);
        if (pendingEvents.isEmpty()) return;

        for (Outbox event : pendingEvents) {
            publishToKafkaAndMarkProcessed(event);
        }
    }

    @Transactional
    public void publishToKafkaAndMarkProcessed(Outbox event) {
        try {
            kafkaTemplate.send(KAFKA_TOPIC, event.getAggregateId(), event.getPayload()).get();

            event.setStatus(OutboxStatus.SENT);
            outboxRepository.save(event);
        } catch (Exception e) {
            System.err.println("Payment outbound pipeline connection failed: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeProcessedOutboxEvents() {
        outboxRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.SENT, LocalDateTime.now().minusDays(1));
    }
}
