-- ===============================
-- PAYMENT TRANSACTIONS
-- ===============================
CREATE TABLE payment_transactions (
    transaction_id        UUID PRIMARY KEY,
    subscription_id       UUID NOT NULL,
    user_id               UUID NOT NULL,
    amount                NUMERIC(12,2) NOT NULL,
    currency              VARCHAR(10) NOT NULL,
    status                VARCHAR(30) NOT NULL,
    provider              VARCHAR(50),
    idempotency_key       VARCHAR(100) UNIQUE NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_tx_user_id
    ON payment_transactions(user_id);

CREATE INDEX idx_payment_tx_subscription_id
    ON payment_transactions(subscription_id);

CREATE INDEX idx_payment_tx_status
    ON payment_transactions(status);
