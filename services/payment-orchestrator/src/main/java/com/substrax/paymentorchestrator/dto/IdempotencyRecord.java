package com.substrax.paymentorchestrator.dto;

public record IdempotencyRecord(
        String transactionId,
        String status,
        String message,
        String requestHash
) {
}
