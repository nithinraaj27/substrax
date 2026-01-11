ALTER TABLE payment_transactions
ADD COLUMN parent_transaction_id UUID;

-- Optional but recommended index
CREATE INDEX idx_payment_parent_tx
ON payment_transactions(parent_transaction_id);
