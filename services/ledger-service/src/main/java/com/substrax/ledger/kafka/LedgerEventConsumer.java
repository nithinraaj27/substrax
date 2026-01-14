package com.substrax.ledger.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substrax.events.ledger.LedgerEvent;
import com.substrax.ledger.entity.LedgerEntry;
import com.substrax.ledger.repository.LedgerRepository;
import com.substrax.ledger.util.AvroJsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;


@Component
@RequiredArgsConstructor
public class LedgerEventConsumer {

    private final LedgerRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "ledger-events",
            groupId = "ledger-service-group"
    )
    @Transactional
    public void consume(LedgerEvent event)
    {
        if (repository.existsByEventId(event.getEventId().toString())) {
            return;
        }

        LedgerEntry entry = LedgerEntry.builder()
                .eventId(event.getEventId().toString())
                .eventType(event.getEventType().toString())
                .ledgerId(event.getLedgerId().toString())
                .transactionId(event.getTransactionId().toString())
                .userId(event.getUserId().toString())
                .amount(BigDecimal.valueOf(event.getAmount()))
                .currency(event.getCurrency().toString())
                .reference(event.getReference().toString())
                .eventTime(event.getEventTime())
                .createdAt(Instant.now())
                .build();

        String rawJson = AvroJsonUtil.toJson(event);
        entry.setRawEvent(rawJson);

        repository.save(entry);
    }
}
