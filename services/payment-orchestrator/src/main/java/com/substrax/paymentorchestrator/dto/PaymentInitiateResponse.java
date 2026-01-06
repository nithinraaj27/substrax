package com.substrax.paymentorchestrator.dto;

public record PaymentInitiateResponse(
        String transactionId,
        String status,
        String message
) {}
