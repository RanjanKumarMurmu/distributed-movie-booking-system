package com.ranjan.payment_microservice.entity;

import com.ranjan.payment_microservice.enumpackage.OutboxStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {
    @Id
    private UUID id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private LocalDateTime createdAt;
}
