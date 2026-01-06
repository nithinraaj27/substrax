package com.substrax.paymentorchestrator.idempotency;

import java.io.Serializable;

public record IdempotencyRecord(String transactionId, String status, String message) implements Serializable {

    private static final long serialVersionID = 1L;
}
