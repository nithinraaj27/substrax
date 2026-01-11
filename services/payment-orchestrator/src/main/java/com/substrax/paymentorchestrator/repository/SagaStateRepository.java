package com.substrax.paymentorchestrator.repository;

import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    Optional<SagaState> findByTransactionId(UUID transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM SagaState s
        WHERE s.currentState = :state
          AND s.updatedAt < :cutoff
          AND s.retryCount < :maxRetries
    """)
    List<SagaState> findTimedOutSagas(
            @Param("state") SagaStatus state,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("maxRetries") int maxRetries
    );
}
