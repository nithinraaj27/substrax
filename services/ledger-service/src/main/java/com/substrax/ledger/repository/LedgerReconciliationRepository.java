package com.substrax.ledger.repository;

import com.substrax.ledger.entity.LedgerReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LedgerReconciliationRepository extends JpaRepository<LedgerReconciliation, Long> {

    Optional<LedgerReconciliation> findByBatchId(String batchId);

    boolean existsByBatchId(String batchId);
}
