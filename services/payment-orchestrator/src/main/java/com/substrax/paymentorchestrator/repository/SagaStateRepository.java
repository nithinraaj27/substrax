package com.substrax.paymentorchestrator.repository;

import com.substrax.paymentorchestrator.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    Optional<SagaState> findByTransactionId(UUID transactionId);
}
