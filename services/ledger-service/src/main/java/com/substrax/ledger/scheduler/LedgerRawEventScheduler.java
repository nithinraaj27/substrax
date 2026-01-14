package com.substrax.ledger.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.substrax.ledger.entity.LedgerEntry;
import com.substrax.ledger.repository.LedgerRepository;
import com.substrax.ledger.util.AvroJsonUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerRawEventScheduler {

    private final LedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @Scheduled(fixedDelay = 30000)
    public void prepareRawEvents(){

        List<LedgerEntry> entries = ledgerRepository.findTop5ByRawEventIsNullOrderByCreatedAtAsc();

        if(entries.isEmpty())
        {
            return;
        }

        for(LedgerEntry entry: entries)
        {
            try{
                String rawJson = AvroJsonUtil.toJson(entry);
                entry.setRawEvent(rawJson);
            }catch (Exception ex)
            {
                log.error("Failed to serialize ledger entry id={}", entry.getId(), ex);
            }

            ledgerRepository.saveAll(entries);

            log.info("Prepared {} raw ledger events", entries.size());
        }
    }
}
