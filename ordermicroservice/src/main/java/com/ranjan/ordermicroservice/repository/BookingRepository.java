package com.ranjan.ordermicroservice.repository;


import com.ranjan.ordermicroservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {}


