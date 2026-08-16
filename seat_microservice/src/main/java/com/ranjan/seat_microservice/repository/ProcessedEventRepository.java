package com.ranjan.seat_microservice.repository;

import com.ranjan.seat_microservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
