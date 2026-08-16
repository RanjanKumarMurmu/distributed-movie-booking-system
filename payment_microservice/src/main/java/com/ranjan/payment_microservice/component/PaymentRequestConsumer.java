package com.ranjan.payment_microservice.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ranjan.payment_microservice.service.PaymentProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final PaymentProcessorService paymentProcessorService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-request-topic", groupId = "payment-engine-group")
    public void consumePaymentRequest(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            // Generate a random unique ID for event wrapper tracing if empty
            UUID eventId = node.has("eventId") ? UUID.fromString(node.get("eventId").asText()) : UUID.randomUUID();
            UUID bookingId = UUID.fromString(node.get("bookingId").asText());

            paymentProcessorService.processMockPayment(eventId, bookingId);
        } catch (Exception e) {
            System.err.println("Failed to orchestrate payment request ingestion: " + e.getMessage());
        }
    }
}
