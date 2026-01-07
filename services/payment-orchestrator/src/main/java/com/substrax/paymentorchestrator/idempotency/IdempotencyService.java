package com.substrax.paymentorchestrator.idempotency;

import com.substrax.paymentorchestrator.dto.IdempotencyDecision;

import java.util.Optional;

public interface IdempotencyService {

    IdempotencyDecision validateAndRegister(String idempotencyKey, String requestHash);

    void markCompleted(String idempotencyKey, String status, String message);

    void markFailed(String idempotencyKey);
}
