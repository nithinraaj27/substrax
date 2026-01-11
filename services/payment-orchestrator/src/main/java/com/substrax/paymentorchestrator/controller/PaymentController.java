package com.substrax.paymentorchestrator.controller;

import com.substrax.common.exception.ApiException;
import com.substrax.paymentorchestrator.dto.IdempotencyDecision;
import com.substrax.paymentorchestrator.dto.PaymentInitiateRequest;
import com.substrax.paymentorchestrator.dto.PaymentInitiateResponse;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.idempotency.IdempotencyService;
import com.substrax.paymentorchestrator.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;
    private final PaymentService paymentService;

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

            PaymentStatus currentStatus;

            try {
                currentStatus = paymentService.getPaymentStatus(decision.transactionId());
            } catch (ApiException ex) {
                // DB row not created yet → eventual consistency
                currentStatus = PaymentStatus.INITIATED;
            }

            return ResponseEntity.ok(
                    new PaymentInitiateResponse(
                            decision.transactionId(),
                            currentStatus.name(),
                            "This request was already processed. Returning existing transaction."
                    )
            );
        }

        // ▶️ FIRST TIME

        UUID transactionId = paymentService.initiatePayment(request, idempotencyKey);

        idempotencyService.markCompleted(
                idempotencyKey,
                PaymentStatus.INITIATED.name(),
                "Payment Initiated Successfully"
        );

        return ResponseEntity.ok(
                new PaymentInitiateResponse(
                        transactionId.toString(),
                        PaymentStatus.INITIATED.name(),
                        "Payment Initiated Successfully"
                )
        );
    }


    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentInitiateResponse> getPaymentStatus(@PathVariable UUID transactionId)
    {
        PaymentStatus status = paymentService.getPaymentStatus(transactionId.toString());

        return ResponseEntity.ok(
                new PaymentInitiateResponse(
                        transactionId.toString(),
                        status.name(),
                        "Payment Status Fetched Successfully"
                )
        );
    }

    @PostMapping("/{txId}/retry")
    public ResponseEntity<?> retry(@PathVariable UUID txId) {
        paymentService.retryPayment(txId);
        PaymentStatus status = paymentService.getPaymentStatus(txId.toString());
        return ResponseEntity.ok(
                new PaymentInitiateResponse(
                        txId.toString(),
                        status.name(),
                        "Retry Payment Successfull"
                )
        );
    }
}
