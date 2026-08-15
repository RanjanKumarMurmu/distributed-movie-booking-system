package com.ranjan.ordermicroservice.controller;

import com.ranjan.ordermicroservice.entity.Booking;
import com.ranjan.ordermicroservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    // Standard constructor injection matching your design style
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Endpoint to initiate a movie booking transaction.
     * Expects booking data in the JSON request body.
     * Returns 201 Created along with the booking payload including its new PENDING status.
     */
    @PostMapping
    public ResponseEntity<Booking> initiateBooking(@RequestBody Booking bookingRequest) {
        try {
            // Hands off the execution to your atomic service layout
            Booking createdBooking = bookingService.createBooking(bookingRequest);
            return new ResponseEntity<>(createdBooking, HttpStatus.CREATED);
        } catch (Exception e) {
            // Logs system faults gracefully and safeguards downstream error trace leaks
            System.err.println("Failed to initiate movie booking payload: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
