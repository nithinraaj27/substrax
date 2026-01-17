package com.substrax.paymentorchestrator.service;

import com.substrax.paymentorchestrator.dto.PaymentInitiateRequest;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import org.apache.kafka.common.protocol.types.Field;

import java.util.UUID;

public interface PaymentService {

    UUID initiatePayment(PaymentInitiateRequest request, String idempotencyKey);

    PaymentStatus getPaymentStatus(String transactionId);

    void retryPayment(UUID txId);


}
