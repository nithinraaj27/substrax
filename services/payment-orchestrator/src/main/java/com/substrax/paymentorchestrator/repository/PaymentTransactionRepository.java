package com.substrax.paymentorchestrator.repository;

import com.substrax.paymentorchestrator.entity.PaymentTransaction;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PaymentTransaction p 
            SET p.status = :newStatus 
            WHERE p.transactionId = :txId 
            AND p.status = :currentStatus
        """)
    int updateStatus(
            @Param("txId") UUID txId,
            @Param("currentStatus") String currentStatus,
            @Param("newStatus") String newStatus
    );

    Optional<PaymentTransaction> findByTransactionId(UUID transactionId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);



}
