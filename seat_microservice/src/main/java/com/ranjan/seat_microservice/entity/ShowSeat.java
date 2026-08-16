package com.ranjan.seat_microservice.entity;



import com.ranjan.seat_microservice.enumpackage.SeatStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "show_seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID showId;
    private String seatNumber; // e.g., "H14"

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    private UUID reservedByBookingId;
    private LocalDateTime lockExpiresAt;

    @Version
    private Long version; // Optimistic locking guard
}

