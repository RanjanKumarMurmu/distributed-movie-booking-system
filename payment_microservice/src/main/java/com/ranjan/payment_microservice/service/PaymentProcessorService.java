package com.ranjan.payment_microservice.service;

import com.ranjan.payment_microservice.entity.Outbox;
import com.ranjan.payment_microservice.entity.PaymentTransaction;
import com.ranjan.payment_microservice.entity.ProcessedEvent;
import com.ranjan.payment_microservice.enumpackage.OutboxStatus;
import com.ranjan.payment_microservice.enumpackage.PaymentStatus;
import com.ranjan.payment_microservice.repository.OutboxRepository;
import com.ranjan.payment_microservice.repository.PaymentRepository;
import com.ranjan.payment_microservice.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentProcessorService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Random random = new Random();

    @Transactional
    public void processMockPayment(UUID eventId, UUID bookingId) {
        // Idempotency check: Don't charge twice if Kafka sends duplicate notifications
        if (processedEventRepository.existsById(eventId)) return;
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));

        // Simulate Gateway Interaction: 90% Success Rate, 10% Declined Simulation
        PaymentStatus finalStatus = (random.nextInt(10) > 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        // 🖨️ Print result cleanly to the system console log
        System.out.println("=================================================================");
        System.out.println(" [MOCK GATEWAY] PROCESSING TRANSACTION FOR BOOKING: " + bookingId);
        System.out.println(" [MOCK GATEWAY] TRANSACTION STATUS RESOLVED: " + finalStatus);
        System.out.println("=================================================================");

        // Persist transaction record locally
        PaymentTransaction tx = new PaymentTransaction();
        tx.setBookingId(bookingId);
        tx.setStatus(finalStatus);
        tx.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(tx);

        // Prepare transactional outbox record payload for downstream consumers
        String jsonPayload = String.format("{\"eventId\":\"%s\",\"bookingId\":\"%s\",\"status\":\"%s\"}",
                UUID.randomUUID(), bookingId, finalStatus.name());

        Outbox outbox = new Outbox();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType("PAYMENT");
        outbox.setAggregateId(bookingId.toString());
        outbox.setEventType("PaymentProcessedEvent");
        outbox.setPayload(jsonPayload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setCreatedAt(LocalDateTime.now());
        outboxRepository.save(outbox);
    }
}
