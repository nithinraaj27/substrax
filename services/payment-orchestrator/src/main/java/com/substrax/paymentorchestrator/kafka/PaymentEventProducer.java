package com.substrax.paymentorchestrator.kafka;

import com.substrax.events.payment.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void send(PaymentEvent event)
    {
        kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event).whenComplete( (result, ex) ->{
            if(ex == null)
            {
                log.info("Event sent Successfully: Offset {}", result.getRecordMetadata());
            }else{
                log.error("Failed to send event", ex);
            }
        });
    }

}
