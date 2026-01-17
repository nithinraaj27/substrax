package com.substrax.ledger.service;

import java.math.BigDecimal;

public interface LedgerReconciliationService {

    void recordResult(String batchId, BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal totalRefunds, Integer recordCount);
}
