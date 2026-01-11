package com.substrax.fraud.events;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.fraud.dto.FraudDecision;
import com.substrax.fraud.dto.FraudResult;
import com.substrax.fraud.service.FraudDecisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final FraudDecisionService fraudDecisionService;
    private final FraudEventProducer fraudEventProducer;

    @Transactional
    @KafkaListener(
            topics = "payment-events",
            groupId = "fraud-service-group"
    )
    public void consume(PaymentEvent event){

        if(event.getEventType() != PaymentEventType.INITIATED)
        {
            return;
        }

        String txId = event.getTransactionId().toString();

        log.info("FraudService recieved INITIATED event, txId={}", event.getTransactionId());

        FraudResult result = fraudDecisionService.evaluate(txId, event.getTransactionId().toString(),BigDecimal.valueOf(event.getAmount()));

        PaymentEventType resultType = result.decision() == FraudDecision.REJECTED ? PaymentEventType.FRAUD_REJECTED : PaymentEventType.FRAUD_APPROVED;

        PaymentEvent resultEvent = PaymentEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(resultType)
                .setTransactionId(event.getTransactionId())
                .setSubscriptionId(event.getSubscriptionId())
                .setUserId(event.getUserId())
                .setAmount(event.getAmount())
                .setCurrency(event.getCurrency())
                .setProvider(event.getProvider())
                .setFailureReason(
                        result.decision() == FraudDecision.REJECTED ? result.reason() : null
                )
                .setEventTime(Instant.now().toEpochMilli())
                .build();

        fraudEventProducer.publish(resultEvent);

        log.info(
                "FraudService published {} for txId={}",
                resultType,
                event.getTransactionId()
        );

    }
}
