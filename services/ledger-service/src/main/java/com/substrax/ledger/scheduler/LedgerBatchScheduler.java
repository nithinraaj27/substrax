    package com.substrax.ledger.scheduler;

    import com.substrax.ledger.dto.LedgerBatchMetadata;
    import com.substrax.ledger.entity.LedgerEntry;
    import com.substrax.ledger.files.LedgerLocalFileWriter;
    import com.substrax.ledger.files.LedgerManifestWriter;
    import com.substrax.ledger.files.LedgerS3Uploader;
    import com.substrax.ledger.repository.LedgerRepository;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.scheduling.annotation.Scheduled;
    import org.springframework.stereotype.Service;

    import java.io.IOException;
    import java.nio.file.Path;
    import java.util.List;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class LedgerBatchScheduler {

        private final LedgerRepository ledgerRepository;
        private final LedgerLocalFileWriter fileWriter;
        private final LedgerS3Uploader s3Uploader;
        private final LedgerManifestWriter manifestWriter;

        private static final int BATCH_SIZE = 6;

        @Transactional
        public void onLedgerInserted(){

            List<LedgerEntry> entries =
                    ledgerRepository.findNextUnbatchedForUpdate(
                            PageRequest.of(0, BATCH_SIZE)
                    );

            if(entries.size() < BATCH_SIZE)
            {
                return;
            }

            String batchId = "LEDGER-BATCH-" + System.currentTimeMillis();

            entries.forEach(e -> e.setBatchId(batchId));
            ledgerRepository.saveAll(entries);

            LedgerBatchMetadata meta = fileWriter.writeBatchToFile(batchId);

            Path manifest = null;
            try {
                manifest = manifestWriter.writeManifest(meta);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            s3Uploader.upload(meta.getDataFile());
            s3Uploader.upload(manifest);

            log.info(
                    "Ledger batch {} exported. Records={}, Hash={}",
                    batchId,
                    meta.getRecordCount(),
                    meta.getSha256()
            );
        }
    }
