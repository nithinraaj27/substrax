package com.substrax.paymentorchestrator.controller;

import com.substrax.common.exception.ApiException;
import com.substrax.paymentorchestrator.dto.PaymentInitiateRequest;
import com.substrax.paymentorchestrator.dto.PaymentInitiateResponse;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.idempotency.IdempotencyRecord;
import com.substrax.paymentorchestrator.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IdempotencyService idempotencyService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> intiatePayment(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PaymentInitiateRequest request
    ){

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException("Idempotency-Key header is required");
        }

        if (request.amount() == null || request.amount() <= 0) {
            throw new ApiException("Amount must be greater than zero");
        }

//        return ResponseEntity.ok(new PaymentInitiateResponse(
//                UUID.randomUUID().toString(),
//                "INITIATED",
//                "Hardcoded test"
//        ));

        return idempotencyService.get(idempotencyKey)
                .map(record -> ResponseEntity.ok(
                        new PaymentInitiateResponse(
                                record.transactionId(),
                                record.status(),
                                record.message()
                        )
                ))
                .orElseGet(() -> {
                    String transactionID = UUID.randomUUID().toString();

                    PaymentInitiateResponse response = new PaymentInitiateResponse(
                            transactionID,
                            PaymentStatus.INITIATED.name(),
                            "Payment initiated successfully"
                    );

                    idempotencyService.save(
                            idempotencyKey,
                            new IdempotencyRecord(
                                    transactionID,
                                    PaymentStatus.INITIATED.name(),
                                    "This payment was already initiated"
                            )
                    );

                    return ResponseEntity.ok(response);
                });
    }
}
