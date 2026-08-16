package com.ranjan.seat_microservice.controller;



import com.ranjan.seat_microservice.entity.ShowSeat;
import com.ranjan.seat_microservice.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final ShowSeatRepository showSeatRepository;

    @GetMapping("/show/{showId}")
    public ResponseEntity<List<ShowSeat>> getSeatLayoutGrid(@PathVariable UUID showId) {
        return ResponseEntity.ok(showSeatRepository.findByShowId(showId));
    }
}

