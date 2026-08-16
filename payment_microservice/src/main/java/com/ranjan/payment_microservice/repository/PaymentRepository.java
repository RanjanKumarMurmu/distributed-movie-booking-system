package com.ranjan.payment_microservice.repository;

import com.ranjan.payment_microservice.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, UUID> {
}
