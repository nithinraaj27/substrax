package com.substrax.paymentorchestrator.service;

import com.substrax.events.payment.PaymentEvent;
import com.substrax.paymentorchestrator.kafka.PaymentEventConsumer;

public interface PaymentSagaService {

    void onFraudApproved(PaymentEvent event);

    void onFraudRejected(PaymentEvent event);
}
