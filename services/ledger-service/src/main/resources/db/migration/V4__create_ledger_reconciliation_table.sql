CREATE TABLE ledger_reconciliation (
    id BIGSERIAL PRIMARY KEY,

    batch_id VARCHAR(100) NOT NULL UNIQUE,

    currency VARCHAR(10) NOT NULL,

    total_debits NUMERIC(18,2) NOT NULL,
    total_credits NUMERIC(18,2) NOT NULL,
    total_refunds NUMERIC(18,2) NOT NULL,

    reconciliation_status VARCHAR(20) NOT NULL,
    -- PASSED | FAILED

    difference NUMERIC(18,2) NOT NULL,
    -- total_debits - (total_credits + total_refunds)

    record_count INT NOT NULL,

    reconciled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
