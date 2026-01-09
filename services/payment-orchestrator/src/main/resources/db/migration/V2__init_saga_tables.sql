-- SAGA STATE

CREATE TABLE saga_state (
     transaction_id        UUID PRIMARY KEY,
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
