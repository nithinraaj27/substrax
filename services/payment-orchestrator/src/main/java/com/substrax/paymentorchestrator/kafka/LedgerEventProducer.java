package com.substrax.paymentorchestrator.kafka;

import com.substrax.events.ledger.LedgerEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "ledger-events";

    public void publish(LedgerEvent event)
    {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event);
    }
}
