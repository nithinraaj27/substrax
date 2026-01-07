package com.substrax.paymentorchestrator.dto;

public record IdempotencyDecision(
        boolean isNewRequest,
        String transactionId,
        String cachedMessage,
        Integer cachedStatus
) {}
