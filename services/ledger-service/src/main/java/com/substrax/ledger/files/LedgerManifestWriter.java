package com.substrax.ledger.files;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.substrax.ledger.dto.LedgerBatchMetadata;
import com.substrax.ledger.scheduler.LedgerBatchScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LedgerManifestWriter {

    private final ObjectMapper objectMapper;

    public Path writeManifest(LedgerBatchMetadata meta) throws IOException {
        Map<String, Object> manifest = Map.of(
                "batchId", meta.getBatchId(),
                "fileName", meta.getDataFile().getFileName().toString(),
                "recordCount", meta.getRecordCount(),
                "currency", meta.getCurrency(),
                "totalAmount", meta.getTotalAmount(),
                "sha256", meta.getSha256(),
                "createdAt", Instant.now().toString()
        );

        Path manifestpath = Path.of(meta.getDataFile().toString()+ ".manifest.json");

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(manifestpath.toFile(), manifest);

        return manifestpath;
    }
}
