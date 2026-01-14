ALTER TABLE ledger_entries
ADD COLUMN reconciliation_status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN reconciled_at TIMESTAMP NULL;
