package com.substrax.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ledger_reconciliation")
@Builder
public class LedgerReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private String batchId;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_debits", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDebits;

    @Column(name = "total_credits", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCredits;

    @Column(name = "total_refunds", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRefunds;

    @Column(name = "difference", nullable = false, precision = 18, scale = 2)
    private BigDecimal difference;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "reconciliation_status", nullable = false, length = 20)
    private String reconciliationStatus;

    @Column(name = "reconciled_at", nullable = false, updatable = false)
    private Instant reconciledAt;
}
