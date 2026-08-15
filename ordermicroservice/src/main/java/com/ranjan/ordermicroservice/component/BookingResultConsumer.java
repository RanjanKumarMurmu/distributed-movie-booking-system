package com.ranjan.ordermicroservice.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ranjan.ordermicroservice.service.BookingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class BookingResultConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    public BookingResultConsumer(BookingService bookingService, ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "booking-result-topic", groupId = "order-service-group")
    public void consumeBookingResult(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            String eventType = rootNode.get("eventType").asText();
            UUID eventId = UUID.fromString(rootNode.get("eventId").asText());
            UUID bookingId = UUID.fromString(rootNode.get("bookingId").asText());

            if ("BookingSuccessEvent".equals(eventType)) {
                bookingService.finalizeBooking(bookingId, eventId);
            }
        } catch (Exception e) {
            System.err.println("Error parsing incoming saga result event: " + e.getMessage());
        }
    }
}
