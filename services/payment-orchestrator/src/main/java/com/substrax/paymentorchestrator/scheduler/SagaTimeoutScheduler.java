package com.substrax.paymentorchestrator.scheduler;

import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import com.substrax.paymentorchestrator.repository.PaymentTransactionRepository;
import com.substrax.paymentorchestrator.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SagaTimeoutScheduler {

    private final SagaStateRepository sagaStateRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 30000)
    public void handleSagaTimeouts(){

        LocalDateTime cutOff = LocalDateTime.now().minusMinutes(5);

        List<SagaState> timedOutSagas = sagaStateRepository.findTimedOutSagas(
                SagaStatus.INITIATED,
                cutOff,
                MAX_RETRIES
        );

        for(SagaState saga: timedOutSagas)
        {
            UUID txId = saga.getTransactionId();

            log.warn("Saga TIMEOUT detected, txId={}", txId);

            saga.setCurrentState(SagaStatus.FAILED_TIMEOUT);
            saga.setLastEvent("RETRY_REQUESTED");
            saga.setCompensationRequired(true);
            sagaStateRepository.save(saga);

            paymentTransactionRepository.updateStatus(
                    txId,
                    PaymentStatus.INITIATED.name(),
                    PaymentStatus.FAILED_TIMEOUT.name()
            );

            log.warn(
                    "Retrying saga txId={} retry={}",
                    saga.getTransactionId(),
                    saga.getRetryCount()
            );
        }

    }
}
