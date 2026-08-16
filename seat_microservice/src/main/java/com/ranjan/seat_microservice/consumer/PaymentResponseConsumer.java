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
public class PaymentResponseConsumer {

    private final SeatAllocationService seatAllocationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-response-topic", groupId = "seat-payment-group")
    public void consumePaymentCallback(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            UUID eventId = UUID.fromString(jsonNode.get("eventId").asText());
            UUID bookingId = UUID.fromString(jsonNode.get("bookingId").asText());
            String paymentStatus = jsonNode.get("status").asText();

            if ("SUCCESS".equalsIgnoreCase(paymentStatus)) {
                seatAllocationService.confirmPermanentBooking(eventId, bookingId);
            } else {
                seatAllocationService.releaseInventoryCompensation(eventId, bookingId);
            }
        } catch (Exception e) {
            System.err.println("Payment Processing Error: " + e.getMessage());
        }
    }
}
