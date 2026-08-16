package com.ranjan.payment_microservice.entity;

import com.ranjan.payment_microservice.enumpackage.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId; // Link back to original Saga boundary context

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime processedAt;
}
