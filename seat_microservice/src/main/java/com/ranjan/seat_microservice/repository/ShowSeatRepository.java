package com.ranjan.seat_microservice.repository;



import com.ranjan.seat_microservice.entity.ShowSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findByShowId(UUID showId);

    // Dynamic database-level lock to look up specific seat arrays safely
    Optional<ShowSeat> findByShowIdAndSeatNumber(UUID showId, String seatNumber);

    // Finds abandoned held rows whose processing window has passed
    @Query("SELECT s FROM ShowSeat s WHERE s.status = 'HELD' AND s.lockExpiresAt < :now")
    List<ShowSeat> findExpiredHolds(@Param("now") LocalDateTime now);

    Optional<ShowSeat> findByReservedByBookingId(UUID bookingId);
}

