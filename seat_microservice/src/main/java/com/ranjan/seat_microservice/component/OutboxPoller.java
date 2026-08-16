package com.ranjan.seat_microservice.component;


import com.ranjan.seat_microservice.entity.Outbox;
import com.ranjan.seat_microservice.entity.ShowSeat;
import com.ranjan.seat_microservice.enumpackage.OutboxStatus;
import com.ranjan.seat_microservice.enumpackage.SeatStatus;
import com.ranjan.seat_microservice.repository.OutboxRepository;
import com.ranjan.seat_microservice.repository.ShowSeatRepository;
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
    private final ShowSeatRepository showSeatRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String PAYMENT_TOPIC = "payment-request-topic";
    private static final String BOOKING_RESULT_TOPIC = "booking-result-topic";

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
            String targetTopic = "PaymentInitiatedEvent".equals(event.getEventType()) ? PAYMENT_TOPIC : BOOKING_RESULT_TOPIC;
            kafkaTemplate.send(targetTopic, event.getAggregateId(), event.getPayload()).get();

            event.setStatus(OutboxStatus.SENT);
            outboxRepository.save(event);
        } catch (Exception e) {
            System.err.println("Seat Service dispatch fault: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 30000) // Cleanup stray timeout reservations every 30 seconds
    @Transactional
    public void releaseAbandonedSeatHolds() {
        List<ShowSeat> expiredHolds = showSeatRepository.findExpiredHolds(LocalDateTime.now());
        for (ShowSeat seat : expiredHolds) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setReservedByBookingId(null);
            seat.setLockExpiresAt(null);
            showSeatRepository.save(seat);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void purgeProcessedOutboxEvents() {
        outboxRepository.deleteByStatusAndCreatedAtBefore(OutboxStatus.SENT, LocalDateTime.now().minusDays(1));
    }
}
