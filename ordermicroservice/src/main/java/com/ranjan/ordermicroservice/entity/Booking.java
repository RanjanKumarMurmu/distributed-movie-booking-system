package com.ranjan.ordermicroservice.entity;



import com.ranjan.ordermicroservice.enumpackage.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userId;
    private String showId;
    private String seatNumbers; // comma-separated string e.g. "A1,A2"
    private double totalAmount;

    @Enumerated(EnumType.STRING)
     private BookingStatus status; // PENDING, CONFIRMED, CANCELLED





}



