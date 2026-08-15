package com.ranjan.ordermicroservice.repository;

import com.ranjan.ordermicroservice.entity.Outbox;
import com.ranjan.ordermicroservice.enumpackage.OutboxStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.*;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {


    @Query(value = "SELECT * FROM outbox WHERE status = :#{#status.name()} ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Outbox> findPendingEventsWithLock(@Param("status") OutboxStatus status, @Param("limit") int limit);


    List<Outbox> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);


    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = :status AND o.createdAt < :time")
    void deleteByStatusAndCreatedAtBefore(@Param("status") OutboxStatus status, @Param("time") LocalDateTime time);
}
