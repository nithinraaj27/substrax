package com.substrax.paymentorchestrator.kafka;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.paymentorchestrator.entity.PaymentStatus;
import com.substrax.paymentorchestrator.entity.SagaState;
import com.substrax.paymentorchestrator.entity.SagaStatus;
import com.substrax.paymentorchestrator.repository.PaymentTransactionRepository;
import com.substrax.paymentorchestrator.repository.SagaStateRepository;
import com.substrax.paymentorchestrator.service.PaymentSagaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentSagaService paymentSagaService;

    @KafkaListener(topics = "payment-events", groupId = "payment-orchestrator-group")
    public void consume(PaymentEvent event)
    {
        log.info("Payment-Events Logs are Consumed from Fraud-Service");

        PaymentEventType eventType = event.getEventType();

        if(eventType == PaymentEventType.FRAUD_APPROVED)
        {

            paymentSagaService.onFraudApproved(event);

        }else if(eventType == PaymentEventType.FRAUD_REJECTED)
        {

            paymentSagaService.onFraudRejected(event);

        }
    }
}
