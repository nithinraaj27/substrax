package com.substrax.paymentorchestrator.serviceimpl;

import com.substrax.events.ledger.LedgerEvent;
import com.substrax.events.ledger.LedgerEventType;
import com.substrax.paymentorchestrator.kafka.LedgerEventProducer;
import com.substrax.paymentorchestrator.service.LedgerEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerServiceImpl implements LedgerEventService {

    private final LedgerEventProducer ledgerEventProducer;

    private static final String ADMIN_LEDGER_ID = "ADMIN_LEDGER";


    @Override
    public void emitLedgerDebit(UUID eventId, String transactionId, String userLedgerId, double amount, String currency, String reference) {
        LedgerEvent event = LedgerEvent.newBuilder()
                .setEventId(eventId.toString())
                .setEventType(LedgerEventType.LEDGER_DEBIT)
                .setLedgerId(userLedgerId.toString())
                .setTransactionId(transactionId)
                .setUserId(userLedgerId.toString())
                .setAmount(amount)
                .setCurrency(currency)
                .setReference(reference)
                .setEventTime(Instant.now().toEpochMilli())
                .build();

        ledgerEventProducer.publish(event);
    }

    @Override
    public void emitLedgerCredit(UUID eventId, String transactionId, double amount, String currency, String reference) {
        LedgerEvent event = LedgerEvent.newBuilder()
                .setEventId(eventId.toString())
                .setEventType(LedgerEventType.LEDGER_CREDIT)
                .setLedgerId(ADMIN_LEDGER_ID)   // 👈 ADMIN
                .setTransactionId(transactionId)
                .setUserId(ADMIN_LEDGER_ID)
                .setAmount(amount)
                .setCurrency(currency)
                .setReference(reference)
                .setEventTime(Instant.now().toEpochMilli())
                .build();

        ledgerEventProducer.publish(event);
    }
}
