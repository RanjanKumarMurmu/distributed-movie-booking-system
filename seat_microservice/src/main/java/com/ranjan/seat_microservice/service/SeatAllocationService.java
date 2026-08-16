package com.ranjan.seat_microservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ranjan.seat_microservice.entity.Outbox;
import com.ranjan.seat_microservice.entity.ProcessedEvent;
import com.ranjan.seat_microservice.entity.ShowSeat;
import com.ranjan.seat_microservice.enumpackage.OutboxStatus;
import com.ranjan.seat_microservice.enumpackage.SeatStatus;
import com.ranjan.seat_microservice.repository.OutboxRepository;
import com.ranjan.seat_microservice.repository.ProcessedEventRepository;
import com.ranjan.seat_microservice.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok generates the constructor injection for all final fields automatically!
public class SeatAllocationService {

    private final ShowSeatRepository showSeatRepository;
    private final OutboxRepository outboxRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void reserveInventoryTemporarily(UUID eventId, UUID bookingId, UUID showId, String seatNumber) {
        if (processedEventRepository.existsById(eventId)) return;
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));

        showSeatRepository.findByShowIdAndSeatNumber(showId, seatNumber).ifPresent(seat -> {
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                // Hold seat for exactly 10 minutes
                seat.setStatus(SeatStatus.HELD);
                seat.setReservedByBookingId(bookingId);
                seat.setLockExpiresAt(LocalDateTime.now().plusMinutes(10));
                showSeatRepository.save(seat);

                // Write Payment Request forward payload into Outbox
                saveToOutbox(bookingId, "PaymentInitiatedEvent", "{\"bookingId\":\"" + bookingId + "\"}");
            } else {
                // Collision scenario: Emit instant fallback cancellation back to booking service loop
                saveToOutbox(bookingId, "BookingFailedEvent", "{\"bookingId\":\"" + bookingId + "\"}");
            }
        });
    }

    @Transactional
    public void confirmPermanentBooking(UUID eventId, UUID bookingId) {
        if (processedEventRepository.existsById(eventId)) return;
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));

        showSeatRepository.findByReservedByBookingId(bookingId).ifPresent(seat -> {
            if (seat.getStatus() == SeatStatus.HELD) {
                seat.setStatus(SeatStatus.LOCKED); // Fully book seat
                seat.setLockExpiresAt(null);
                showSeatRepository.save(seat);

                saveToOutbox(bookingId, "BookingSuccessEvent", "{\"bookingId\":\"" + bookingId + "\"}");
            }
        });
    }

    @Transactional
    public void releaseInventoryCompensation(UUID eventId, UUID bookingId) {
        if (processedEventRepository.existsById(eventId)) return;
        processedEventRepository.save(new ProcessedEvent(eventId, LocalDateTime.now()));

        showSeatRepository.findByReservedByBookingId(bookingId).ifPresent(seat -> {
            seat.setStatus(SeatStatus.AVAILABLE); // Release seat back to market
            seat.setReservedByBookingId(null);
            seat.setLockExpiresAt(null);
            showSeatRepository.save(seat);

            saveToOutbox(bookingId, "BookingFailedEvent", "{\"bookingId\":\"" + bookingId + "\"}");
        });
    }

    private void saveToOutbox(UUID bookingId, String eventType, String jsonPayload) {
        Outbox outbox = new Outbox();
        outbox.setId(UUID.randomUUID());
        outbox.setAggregateType("SEAT");
        outbox.setAggregateId(bookingId.toString());
        outbox.setEventType(eventType);
        outbox.setPayload(jsonPayload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setCreatedAt(LocalDateTime.now());
        outboxRepository.save(outbox);
    }
}
