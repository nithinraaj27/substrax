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

-- ===============================
-- SAGA STATE
-- ===============================
CREATE TABLE saga_state (
    saga_id               UUID PRIMARY KEY,
    transaction_id        UUID NOT NULL,
    current_state         VARCHAR(30) NOT NULL,
    last_event            VARCHAR(50),
    compensation_required BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saga_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES payment_transactions(transaction_id)
);

CREATE INDEX idx_saga_transaction_id
    ON saga_state(transaction_id);

CREATE INDEX idx_saga_current_state
    ON saga_state(current_state);
