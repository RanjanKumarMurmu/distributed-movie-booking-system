package com.ranjan.seat_microservice.repository;


import com.ranjan.seat_microservice.entity.Outbox;
import com.ranjan.seat_microservice.enumpackage.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    @Query(value = "SELECT * FROM seat_outbox WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> findPendingEventsWithLock(@Param("status") OutboxStatus status, @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = :status AND o.createdAt < :time")
    void deleteByStatusAndCreatedAtBefore(@Param("status") OutboxStatus status, @Param("time") LocalDateTime time);
}
