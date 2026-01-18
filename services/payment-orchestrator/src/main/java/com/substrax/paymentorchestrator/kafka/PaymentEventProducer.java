package com.substrax.paymentorchestrator.kafka;

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
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Retry(name = "paymentProducer", fallbackMethod = "sendFallback")
    @CircuitBreaker(name = "paymentProducer", fallbackMethod = "sendFallback")
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


    public void sendFallback(PaymentEvent event, Exception ex) {
        log.error(
                "Kafka unavailable. Payment event NOT sent. txId={} eventType={}",
                event.getTransactionId(),
                event.getEventType(),
                ex
        );
        // DO NOTHING — saga timeout will handle
    }

}
