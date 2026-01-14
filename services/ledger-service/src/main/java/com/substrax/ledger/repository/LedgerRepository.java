package com.substrax.ledger.repository;

import com.substrax.ledger.entity.LedgerEntry;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    boolean existsByEventId(String eventId);

    List<LedgerEntry> findTop5ByRawEventIsNullOrderByCreatedAtAsc();

    List<LedgerEntry> findTop5ByRawEventIsNotNullAndExportedFalseOrderByCreatedAtAsc();

    List<LedgerEntry> findByBatchIdAndExportedFalse(String batchId);

    @Query("""
            SELECT DISTINCT l.batchId
            FROM LedgerEntry l
            WHERE l.batchId IS NOT NULL
                AND l.exported = false
        """)
    List<String> findDistinctUnexportedBatchIds();

    @Modifying
    @Transactional
    @Query("""
            UPDATE LedgerEntry l
            SET l.exported = true,
            l.exportedAt = CURRENT_TIMESTAMP
            WHERE l.batchId = :batchId
        """)
    void markBatchExported(@Param("batchId") String batchId);



}
