package com.substrax.paymentorchestrator.controller;

import com.substrax.common.exception.ApiException;
import com.substrax.paymentorchestrator.dto.IdempotencyDecision;
import com.substrax.paymentorchestrator.dto.PaymentInitiateRequest;
import com.substrax.paymentorchestrator.dto.PaymentInitiateResponse;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentInitiateRequest request
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException("Idempotency-Key header is required");
        }

        if (request.amount() == null || request.amount() <= 0) {
            throw new ApiException("Amount must be greater than zero");
        }

        String requestHash = DigestUtils.sha256Hex(
                "POST|/payments/initiate|" +
                        request.amount() + "|" +
                        request.currency() + "|" +
                        request.userId()
        );

        IdempotencyDecision decision =
                idempotencyService.validateAndRegister(idempotencyKey, requestHash);

        // 🔁 REPLAY
        if (!decision.isNewRequest()) {
            return ResponseEntity.ok(
                    new PaymentInitiateResponse(
                            decision.transactionId(),
                            PaymentStatus.INITIATED.name(),
                            "This request was already processed. Returning existing transaction."
                    )
            );
        }

        // ▶️ FIRST TIME
        idempotencyService.markCompleted(
                idempotencyKey,
                PaymentStatus.INITIATED.name(),
                "Payment Initiated Successfully"
        );

        return ResponseEntity.ok(
                new PaymentInitiateResponse(
                        decision.transactionId(),
                        PaymentStatus.INITIATED.name(),
                        "Payment Initiated Successfully"
                )
        );
    }
}
