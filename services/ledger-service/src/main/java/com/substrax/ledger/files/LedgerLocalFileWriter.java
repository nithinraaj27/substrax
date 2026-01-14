package com.substrax.ledger.files;

import com.substrax.ledger.entity.LedgerEntry;
import com.substrax.ledger.repository.LedgerRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerLocalFileWriter {

    private final LedgerRepository ledgerRepository;

    @Value("${ledger.export.local-path:/data/ledger}")
    private String exportPath;

    @Transactional(readOnly = true)
    public Path writeBatchToFile(String batchId){
        List<LedgerEntry> entries = ledgerRepository.findByBatchIdAndExportedFalse(batchId);

        if(entries.isEmpty())
        {
            throw  new IllegalStateException("No entries found for batch"+ batchId);
        }

        try{
            Files.createDirectories(Path.of(exportPath));

            String filename = "ledger-" + batchId + "-" + Instant.now().toEpochMilli() + ".json";
            Path filepath = Path.of(exportPath, filename);

            try(BufferedWriter writer = Files.newBufferedWriter(filepath))
            {
                for(LedgerEntry entry: entries)
                {
                    writer.write(entry.getRawEvent().toString());
                    writer.newLine();
                }
            }

            log.info("Ledger batch written to Local files: {}", filepath);
            return  filepath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to write ledger batch file", e);
        }
    }

}
