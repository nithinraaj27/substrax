package com.substrax.paymentorchestrator.idempotency;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IdempotencyRecord(
        String transactionId,
        String status,
        String message,
        String requestHash,
        IdempotencyState state
){
    public static IdempotencyRecord inProgress(String transactionId, String requestHash){
        return new IdempotencyRecord(transactionId, null, null, requestHash ,IdempotencyState.COMPLETED);
    }

    public boolean isCompleted(){
        return this.state == IdempotencyState.COMPLETED;
    }

}
