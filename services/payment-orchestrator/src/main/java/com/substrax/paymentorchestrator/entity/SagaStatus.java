package com.substrax.paymentorchestrator.entity;

public enum SagaStatus {
    INITIATED,
    COMPLETED,
    FAILED,
    FAILED_TIMEOUT,
    RETRYING
}
