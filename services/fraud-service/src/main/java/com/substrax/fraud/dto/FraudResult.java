package com.substrax.fraud.dto;

public record FraudResult(
        String transactionId,
        FraudDecision decision,
        String reason
) {
}
