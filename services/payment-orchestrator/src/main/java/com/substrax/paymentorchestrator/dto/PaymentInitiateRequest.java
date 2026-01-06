package com.substrax.paymentorchestrator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PaymentInitiateRequest(
        @NotBlank
        String subscriptionId,

        @NotBlank
        String userId,

        @Positive
        Double amount,

        @NotBlank
        String currency,

        @NotBlank
        String provider
) {}
