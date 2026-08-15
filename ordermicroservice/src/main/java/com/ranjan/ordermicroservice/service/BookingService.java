package com.ranjan.ordermicroservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ranjan.ordermicroservice.entity.Booking;
import com.ranjan.ordermicroservice.entity.Outbox;
import com.ranjan.ordermicroservice.entity.ProcessedEvent;
import com.ranjan.ordermicroservice.enumpackage.BookingStatus;
import com.ranjan.ordermicroservice.enumpackage.OutboxStatus;
import com.ranjan.ordermicroservice.repository.BookingRepository;
import com.ranjan.ordermicroservice.repository.OutboxRepository;
import com.ranjan.ordermicroservice.repository.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    // Unified constructor injecting all 3 required repositories and Jackson serializer
    public BookingService(BookingRepository bookingRepository,
                          OutboxRepository outboxRepository,
                          ProcessedEventRepository processedEventRepository,
                          ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.outboxRepository = outboxRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * PHASE 1: One-Time Producer Action
     * Executed when a user initially requests a ticket purchase.
     * Atomically saves the state and records the Outbox event in the same DB transaction.
     */
    @Transactional
    public Booking createBooking(Booking request) {
        // 1. Persist the booking state as PENDING
        request.setStatus(BookingStatus.PENDING);
        Booking savedBooking = bookingRepository.save(request);

        // 2. Wrap the payload data for the event
        UUID eventId = UUID.randomUUID();
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(savedBooking);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed for Outbox payload", e);
        }

        // 3. Persist the event entry to the Outbox table using explicit setters
        Outbox outboxEntry = new Outbox();
        outboxEntry.setId(eventId);
        outboxEntry.setAggregateType("BOOKING");
        outboxEntry.setAggregateId(savedBooking.getId().toString());
        outboxEntry.setEventType("BookingCreatedEvent");
        outboxEntry.setPayload(jsonPayload);
        outboxEntry.setStatus(OutboxStatus.PENDING);
        outboxEntry.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(outboxEntry);

        return savedBooking;
    }

    /**
     * PHASE 2: One-Time Consumer Action
     * Executed when downstream microservices (Seat & Payment) successfully process the request.
     * Atomically updates state while ensuring the message isn't processed a second time.
     */
    @Transactional
    public void finalizeBooking(UUID bookingId, UUID incomingEventId) {
        // 1. Idempotency Check: Guard against duplicate network retries from Kafka
        if (processedEventRepository.existsById(incomingEventId)) {
            // Log warning or return silently; message has already been handled perfectly
            return;
        }

        // 2. De-duplicate immediately by logging the unique incoming Kafka event ID
        processedEventRepository.save(new ProcessedEvent(incomingEventId, LocalDateTime.now()));

        // 3. Update the transactional state to CONFIRMED safely
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
            }
        });
    }

    /**
     * COMPENSATING TRANSACTION: (Optional but recommended for robust Saga patterns)
     * Executed if downstream processing fails (e.g., payment declined or seat locking timeout).
     */
    @Transactional
    public void cancelBooking(UUID bookingId, UUID incomingEventId) {
        if (processedEventRepository.existsById(incomingEventId)) {
            return;
        }

        processedEventRepository.save(new ProcessedEvent(incomingEventId, LocalDateTime.now()));

        bookingRepository.findById(bookingId).ifPresent(booking -> {
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        });
    }
}
