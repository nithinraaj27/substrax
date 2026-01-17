package com.substrax.paymentorchestrator.serviceimpl;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import com.substrax.paymentorchestrator.kafka.PaymentEventProducer;
import com.substrax.paymentorchestrator.repository.PaymentTransactionRepository;
import com.substrax.paymentorchestrator.repository.SagaStateRepository;
import com.substrax.paymentorchestrator.service.LedgerEventService;
import com.substrax.paymentorchestrator.service.PaymentSagaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaServiceImpl implements PaymentSagaService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SagaStateRepository sagaStateRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final LedgerEventService ledgerEventService;

    private static final int MAX_RETRIES = 3;
    private static final Duration TIMEOUT_DURATION = Duration.ofMinutes(3);

    @Transactional
    @Override
    public void onFraudApproved(PaymentEvent event) {

        UUID txId = UUID.fromString(event.getTransactionId().toString());

        SagaState saga = sagaStateRepository.findByTransactionId(txId).orElseThrow();

        // Late / duplicate event protection
        if (saga.getCurrentState() == SagaStatus.COMPLETED ||
                saga.getCurrentState() == SagaStatus.FAILED ||
                saga.getCurrentState() == SagaStatus.FAILED_TIMEOUT) {

            log.warn("Ignoring late FRAUD_APPROVED for txId={} sagaState={}",
                    txId, saga.getCurrentState());
            return;
        }

        // 1 Update Saga
        saga.setCurrentState(SagaStatus.COMPLETED);
        saga.setLastEvent(PaymentEventType.FRAUD_APPROVED.name());
        saga.setCompensationRequired(false);
        sagaStateRepository.save(saga);

        // 2. Update payment transaction to FINAL SUCCESS
        int updated = paymentTransactionRepository.updateStatus(
                txId,
                PaymentStatus.INITIATED.name(),
                PaymentStatus.COMPLETED.name()
        );
        log.info("Payment Approved and Saved {}", event.getTransactionId());


        if (updated == 0) {
            log.warn("Payment already finalized, skipping ledger event. txId={}", txId);
            return;
        }

        ledgerEventService.emitLedgerDebit(
                UUID.randomUUID(),
                event.getTransactionId().toString(),
                event.getUserId().toString(),
                event.getAmount(),
                event.getCurrency().toString(),
                "PAYMENT_SUCCESS"
        );

        ledgerEventService.emitLedgerCredit(
                UUID.randomUUID(),
                event.getTransactionId().toString(),        // admin / platform
                event.getAmount(),
                event.getCurrency().toString(),
                "PAYMENT_SUCCESS"
        );
    }

    @Transactional
    @Override
    public void onFraudRejected(PaymentEvent event) {

        UUID txId = UUID.fromString(event.getTransactionId().toString());

        SagaState saga = sagaStateRepository.findByTransactionId(txId).orElseThrow(
                () -> new IllegalArgumentException("Illegal Transaction ID"));

        // Late / duplicate event protection
        if (saga.getCurrentState() == SagaStatus.COMPLETED ||
                saga.getCurrentState() == SagaStatus.FAILED ||
                saga.getCurrentState() == SagaStatus.FAILED_TIMEOUT) {

            log.warn("Ignoring late FRAUD_REJECTED for txId={} sagaState={}",
                    txId, saga.getCurrentState());
            return;
        }

        //1 Update SAGA
        saga.setCurrentState(SagaStatus.FAILED);
        saga.setLastEvent(PaymentEventType.FRAUD_REJECTED.name());
        saga.setCompensationRequired(true);
        sagaStateRepository.save(saga);

        log.info("Payment Rejected and Compensation will be given {}", event.getTransactionId());

        // 2 Update payment transaction
        int updated = paymentTransactionRepository.updateStatus(
                txId,
                PaymentStatus.INITIATED.name(),   // currentStatus (MUST MATCH DB)
                PaymentStatus.FAILED.name()      // newStatus
        );

        if(updated == 0)
        {
            log.warn("Payment already finalized, txId={}", txId);
            return;
        }

        // 3 Emit Payment_failed
        PaymentEvent failedEvent = PaymentEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(PaymentEventType.PAYMENT_FAILED)
                .setTransactionId(event.getTransactionId().toString())
                .setSubscriptionId(event.getSubscriptionId())
                .setUserId(event.getUserId())
                .setAmount(event.getAmount())
                .setCurrency(event.getCurrency())
                .setProvider(event.getProvider())
                .setFailureReason(event.getFailureReason())
                .setEventTime(Instant.now().toEpochMilli())
                .build();

        paymentEventProducer.send(failedEvent);
    }

    @Transactional
    @Override
    public void retryTimeoutSagas() {

        LocalDateTime cutoff =
                LocalDateTime.now().minus(TIMEOUT_DURATION);

        List<SagaState> timedOutSagas =
                sagaStateRepository.findTimedOutSagas(
                        SagaStatus.FAILED_TIMEOUT,
                        cutoff,
                        MAX_RETRIES
                );

        for(SagaState saga: timedOutSagas)
        {
            saga.incrementRetryCount();
            saga.markRetrying();
            sagaStateRepository.save(saga);

            PaymentEvent retryEvent = PaymentEvent.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setEventType(PaymentEventType.INITIATED)
                    .setTransactionId(saga.getTransactionId().toString())
                    .setEventTime(Instant.now().toEpochMilli())
                    .build();

            paymentEventProducer.send(retryEvent);

            log.warn(
                    "Retrying saga txId={} retry={}",
                    saga.getTransactionId(),
                    saga.getRetryCount()
            );
        }
    }
}
