package com.substrax.fraud.events;

import com.substrax.events.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FraudEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event)
    {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event);
    }
}
