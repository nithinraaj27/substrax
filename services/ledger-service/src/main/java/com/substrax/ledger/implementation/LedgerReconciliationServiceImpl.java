package com.substrax.ledger.implementation;

import com.substrax.ledger.entity.LedgerReconciliation;
import com.substrax.ledger.enusms.ReconciliationStatus;
import com.substrax.ledger.repository.LedgerReconciliationRepository;
import com.substrax.ledger.service.LedgerReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerReconciliationServiceImpl implements LedgerReconciliationService {

    private final LedgerReconciliationRepository repository;

    @Override
    public void recordResult(String batchId, BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal totalRefunds, Integer recordCount) {

        if(repository.existsByBatchId(batchId))
        {
            log.warn("Reconciliation already exists for batchId={}", batchId);
            return;
        }

        BigDecimal difference = totalDebit.subtract(totalCredit).subtract(totalRefunds);

        String status = difference.compareTo(BigDecimal.ZERO) == 0 ? ReconciliationStatus.BALANCED.name() : ReconciliationStatus.MISMATCH.name();

        LedgerReconciliation reconciliation = LedgerReconciliation.builder()
                .batchId(batchId)
                .totalDebits(totalDebit)
                .totalCredits(totalCredit)
                .totalRefunds(totalCredit)
                .difference(difference)
                .recordCount(recordCount)
                .reconciliationStatus(status)
                .reconciledAt(Instant.now())
                .build();

        repository.save(reconciliation);

        log.info(
                "Ledger reconciliation saved | batchId={} status={} difference={}",
                batchId, status, difference
        );
    }
}
