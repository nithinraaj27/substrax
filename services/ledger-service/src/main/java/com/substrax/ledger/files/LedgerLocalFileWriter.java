package com.substrax.ledger.files;

import com.substrax.ledger.dto.LedgerBatchMetadata;
import com.substrax.ledger.entity.LedgerEntry;
import com.substrax.ledger.repository.LedgerRepository;
import com.substrax.ledger.scheduler.LedgerBatchScheduler;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerLocalFileWriter {

    private final LedgerRepository ledgerRepository;

    @Value("${ledger.export.local-path:/data/ledger}")
    private String exportPath;

    @Transactional(readOnly = true)
    public LedgerBatchMetadata writeBatchToFile(String batchId) {
        List<LedgerEntry> entries = ledgerRepository.findByBatchIdAndExportedFalse(batchId);

        if (entries.isEmpty()) {
            throw new IllegalStateException("No entries found for batch" + batchId);
        }

        try {
            Files.createDirectories(Path.of(exportPath));

            String filename = "ledger-" + batchId + "-" + Instant.now().toEpochMilli() + ".json";
            Path filepath = Path.of(exportPath, filename);

            BigDecimal totalAmount = BigDecimal.ZERO;

            try (BufferedWriter writer = Files.newBufferedWriter(filepath, StandardCharsets.UTF_8)) {
                for (LedgerEntry entry : entries) {
                    writer.write(entry.getRawEvent());
                    writer.newLine();
                    totalAmount = totalAmount.add(entry.getAmount());
                }
            }

            byte[] bytes = Files.readAllBytes(filepath);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String sha256 = HexFormat.of().formatHex(digest.digest(bytes));

            entries.forEach(entry -> entry.setExported(true));
            ledgerRepository.saveAll(entries);

            log.info("Ledger batch {} exported successfully. Entries={}, File={}", batchId, entries.size(), filepath);

            return new LedgerBatchMetadata(
                    batchId,
                    filepath,
                    entries.size(),
                    totalAmount,
                    entries.get(0).getCurrency(),
                    sha256
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to write ledger batch file", e);
        }
    }

}
