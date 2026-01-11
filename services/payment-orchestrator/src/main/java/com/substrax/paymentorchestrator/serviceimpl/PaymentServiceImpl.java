package com.substrax.paymentorchestrator.serviceimpl;

import com.substrax.common.exception.ApiException;
import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.paymentorchestrator.dto.PaymentInitiateRequest;
import com.substrax.paymentorchestrator.dto.PaymentInitiateResponse;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.entity.PaymentTransaction;
import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import com.substrax.paymentorchestrator.kafka.PaymentEventProducer;
import com.substrax.paymentorchestrator.repository.PaymentTransactionRepository;
import com.substrax.paymentorchestrator.repository.SagaStateRepository;
import com.substrax.paymentorchestrator.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentTransactionRepository transactionRepository;
    private final SagaStateRepository sagaStateRepository;
    private final PaymentEventProducer eventProducer;

    @Override
    public UUID initiatePayment(PaymentInitiateRequest request, String idempotencyKey) {

        Optional<PaymentTransaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);

        if(existing.isPresent())
        {
            log.info("Idempotent replay detected for key={} , txId={}", idempotencyKey, existing.get().getTransactionId());

            return existing.get().getTransactionId();
        }


        UUID transactionId = UUID.randomUUID();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionId(transactionId)
                .subscriptionId(UUID.fromString(request.subscriptionId()))
                .userId(UUID.fromString(request.userId()))
                .amount(BigDecimal.valueOf(request.amount()))
                .currency(request.currency())
                .status(PaymentStatus.INITIATED.name())
                .provider(request.provider())
                .idempotencyKey(idempotencyKey)
                .build();

        transactionRepository.save(transaction);

        SagaState saga = new SagaState();
        saga.setTransactionId(transactionId);
        saga.setCurrentState(SagaStatus.INITIATED);
        saga.setLastEvent(PaymentEventType.INITIATED.name());
        saga.setCompensationRequired(false);

        sagaStateRepository.save(saga);

        PaymentEvent event = PaymentEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(PaymentEventType.INITIATED)
                .setTransactionId(transaction.getTransactionId().toString())
                .setSubscriptionId(request.subscriptionId())
                .setUserId(request.userId())
                .setAmount(request.amount())
                .setCurrency(request.currency())
                .setProvider(request.provider())
                .setFailureReason(null)
                .setEventTime(Instant.now().toEpochMilli())
                .build();

        eventProducer.send(event);
        log.info("Events Sent to the Topic {}", transactionId);
        return transactionId;
    }

    @Override
    public PaymentStatus getPaymentStatus(String transactionId) {

        String status = transactionRepository.findByTransactionId(UUID.fromString(transactionId))
                .orElseThrow(() -> new ApiException("Payment Not found for transactionId="+transactionId)).getStatus();

        return PaymentStatus.valueOf(status);
    }

    @Transactional
    @Override
    public void retryPayment(UUID txId) {

        SagaState oldSaga = sagaStateRepository.findByTransactionId(txId)
                .orElseThrow(() -> new IllegalStateException("Saga not found"));

        if (oldSaga.getCurrentState() != SagaStatus.FAILED_TIMEOUT) {
            throw new IllegalStateException("Retry allowed only for FAILED_TIMEOUT");
        }

        if (oldSaga.getRetryCount() >= 3) {
            throw new IllegalStateException("Max retries exhausted");
        }

        // 2️⃣ Increment retry count on OLD saga
        oldSaga.incrementRetryCount();
        sagaStateRepository.save(oldSaga);

        // 3️⃣ Create NEW transaction
        PaymentTransaction oldTx =
                transactionRepository.findByTransactionId(txId)
                        .orElseThrow();

        UUID newTxId = UUID.randomUUID();

        PaymentTransaction retryTx = PaymentTransaction.builder()
                .transactionId(newTxId)
                .parentTransactionId(txId)
                .subscriptionId(oldTx.getSubscriptionId())
                .userId(oldTx.getUserId())
                .amount(oldTx.getAmount())
                .currency(oldTx.getCurrency())
                .provider(oldTx.getProvider())
                .status(PaymentStatus.INITIATED.name())
                .idempotencyKey(oldTx.getIdempotencyKey() + "-retry-" + oldSaga.getRetryCount())
                .build();

        transactionRepository.save(retryTx);

        // 4️⃣ Create NEW saga
        SagaState retrySaga = SagaState.builder()
                .transactionId(newTxId)
                .currentState(SagaStatus.INITIATED)
                .lastEvent("RETRY_INITIATED")
                .retryCount(0)
                .compensationRequired(false)
                .build();

        sagaStateRepository.save(retrySaga);

        // 5️⃣ Publish INITIATED event
        eventProducer.send(
                PaymentEvent.newBuilder()
                        .setEventId(UUID.randomUUID().toString())
                        .setEventType(PaymentEventType.INITIATED)
                        .setTransactionId(newTxId.toString())
                        .setSubscriptionId(oldTx.getSubscriptionId().toString()) // ✅ REQUIRED
                        .setUserId(oldTx.getUserId().toString())
                        .setAmount(oldTx.getAmount().doubleValue())
                        .setCurrency(oldTx.getCurrency())
                        .setProvider(oldTx.getProvider())
                        .setEventTime(Instant.now().toEpochMilli())
                        .build()
        );
    }
}
