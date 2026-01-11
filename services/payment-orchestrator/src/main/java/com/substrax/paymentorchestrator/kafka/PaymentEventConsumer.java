package com.substrax.paymentorchestrator.kafka;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import com.substrax.paymentorchestrator.repository.PaymentTransactionRepository;
import com.substrax.paymentorchestrator.repository.SagaStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentEventConsumer {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @KafkaListener(topics = "payment-events", groupId = "payment-orchestrator-group")
    public void consume(PaymentEvent event)
    {
        log.info("Payment-Events Logs are Consumed from Fraud-Service");


        PaymentEventType eventType = event.getEventType();

        if(eventType != PaymentEventType.FRAUD_APPROVED && eventType != PaymentEventType.FRAUD_REJECTED)
        {
            return;
        }

        UUID transactionId = null;

        try{
            transactionId = UUID.fromString(event.getTransactionId().toString());
        }catch(Exception ex)
        {
            log.error("Invalid transactionId received: {}", event.getTransactionId());
            return;
        }

        UUID finalTransactionId = transactionId;
        SagaState saga = sagaStateRepository.findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new IllegalStateException("Saga not found for txId={}" + finalTransactionId)
                );

        // ✅ LATE EVENT PROTECTION (NO enum change)
        if (saga.getCurrentState() == SagaStatus.FAILED_TIMEOUT ||
                saga.getCurrentState() == SagaStatus.COMPLETED ||
                saga.getCurrentState() == SagaStatus.FAILED) {

            log.warn(
                    "Late fraud event ignored txId={} sagaState={} event={}",
                    transactionId,
                    saga.getCurrentState(),
                    eventType
            );
            return;
        }


        switch(eventType) {
            case FRAUD_APPROVED -> {
                // 1. Update Saga
                saga.setCurrentState(SagaStatus.COMPLETED);
                saga.setLastEvent(eventType.name());
                sagaStateRepository.save(saga);

                // 2. Update Payment: INITIATED -> COMPLETED
                int rows = paymentTransactionRepository.updateStatus(
                        transactionId,
                        PaymentStatus.INITIATED.name(), // currentStatus
                        PaymentStatus.COMPLETED.name()   // newStatus
                );

                if (rows > 0) {
                    log.info("Payment SUCCESS: INITIATED -> COMPLETED for txId: {}", transactionId);
                } else {
                    log.warn("Approval ignored. Payment {} not in INITIATED state.", transactionId);
                }
            }

            case FRAUD_REJECTED -> {
                // 1. Update Saga
                saga.setCurrentState(SagaStatus.FAILED);
                saga.setLastEvent(eventType.name());
                sagaStateRepository.save(saga);

                // 2. Update Payment: INITIATED -> FAILED
                int rows = paymentTransactionRepository.updateStatus(
                        transactionId,
                        PaymentStatus.INITIATED.name(), // currentStatus
                        PaymentStatus.FAILED.name()      // newStatus
                );

                if (rows > 0) {
                    log.info("Payment FAILED: INITIATED -> FAILED for txId: {}", transactionId);
                } else {
                    log.warn("Rejection ignored. Payment {} not in INITIATED state.", transactionId);
                }
            }
        }
    }
}
