package com.substrax.paymentorchestrator.idempotency;

import java.util.Optional;

public interface IdempotencyService {

    Optional<IdempotencyRecord> get(String key);

    void save(String key, IdempotencyRecord record);
}
