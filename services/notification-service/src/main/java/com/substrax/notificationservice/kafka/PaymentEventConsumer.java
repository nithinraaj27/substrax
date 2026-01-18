package com.substrax.notificationservice.kafka;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.events.payment.PaymentEventType;
import com.substrax.notificationservice.dto.NotificationStatus;
import com.substrax.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "notification-service-group"
    )
    public void consumePaymentEvent(PaymentEvent event){

        log.info("Received PaymentEvent: {}", event.getEventType());

        if(event.getEventType() == PaymentEventType.PAYMENT_SUCCESS)
        {
            notificationService.createNotification(
                    event.getEventId().toString(),
                    event.getUserId().toString(),
                    PaymentEventType.PAYMENT_SUCCESS.name(),
                    buildMessage(event)
            );
        }
        else if(event.getEventType() == PaymentEventType.PAYMENT_FAILED)
        {
            notificationService.createNotification(
                    event.getEventId().toString(),
                    event.getUserId().toString(),
                    PaymentEventType.PAYMENT_FAILED.name(),
                    buildMessage(event)
            );
        }
    }

    private String buildMessage(PaymentEvent event) {
        return switch (event.getEventType()) {
            case PAYMENT_FAILED ->
                    "Your payment has failed. Please try again.";
            case PAYMENT_SUCCESS ->
                    "Your payment was successful.";
            default ->
                    "Payment update received.";
        };
    }
}
