package com.substrax.fraud.events;

import com.substrax.events.payment.PaymentEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @CircuitBreaker(name="fraudservice", fallbackMethod = "fraudFallback")
    @Retry(name = "fraudService")
    public void publish(PaymentEvent event)
    {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event);
    }

    public void fraudFallback(PaymentEvent event, Exception ex) {
        log.error("Kafka unavailable. Fraud decision not published. txId={}", event.getTransactionId(), ex);

        // Do nothing
        // Payment-Orchestrator timeout will handle it
    }
}
