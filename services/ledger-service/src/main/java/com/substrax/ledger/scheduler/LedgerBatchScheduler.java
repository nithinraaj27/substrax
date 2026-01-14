package com.substrax.ledger.scheduler;

import com.substrax.ledger.entity.LedgerEntry;
import com.substrax.ledger.files.LedgerLocalFileWriter;
import com.substrax.ledger.files.LedgerS3Uploader;
import com.substrax.ledger.repository.LedgerRepository;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerBatchScheduler {

    private final LedgerRepository ledgerRepository;
    private final LedgerLocalFileWriter fileWriter;
    private final LedgerS3Uploader s3Uploader;

    @Transactional
    @Scheduled(fixedDelay = 60000)
    public void createBatch()
    {
        List<LedgerEntry> entries = ledgerRepository.findTop5ByRawEventIsNotNullAndExportedFalseOrderByCreatedAtAsc();

        if(entries.isEmpty())
        {
            return;
        }

        String batchId = "LEDGER-BATCH-"+ System.currentTimeMillis();

        for(LedgerEntry entry: entries)
        {
            entry.setBatchId(batchId);
        }

        ledgerRepository.saveAll(entries);

        log.info("Ledger Batch created: {} with {} entries", batchId, entries.size());
    }


    @Scheduled(fixedDelay = 90000)
    @Transactional
    public void prepareBatchFile(){

        List<String> batchIds = ledgerRepository.findDistinctUnexportedBatchIds();

        for(String batchId: batchIds)
        {
            Path file = fileWriter.writeBatchToFile(batchId);
            s3Uploader.upload(file);
            ledgerRepository.markBatchExported(batchId);
            log.info("Ledger batch {} marked as exported", batchId);
        }
    }
}
