package com.ranjan.seat_microservice.consumer;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ranjan.seat_microservice.service.SeatAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingEventsConsumer {

    private final SeatAllocationService seatAllocationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booking-events-topic", groupId = "seat-booking-group")
    public void consumeInitialBooking(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(jsonNode.get("eventId").asText());
            UUID bookingId = UUID.fromString(jsonNode.get("id").asText());
            UUID showId = UUID.fromString(jsonNode.get("showId").asText());
            String seatNumber = jsonNode.get("seatNumber").asText();

            seatAllocationService.reserveInventoryTemporarily(eventId, bookingId, showId, seatNumber);
        } catch (Exception e) {
            System.err.println("Booking Event Error: " + e.getMessage());
        }
    }
}

