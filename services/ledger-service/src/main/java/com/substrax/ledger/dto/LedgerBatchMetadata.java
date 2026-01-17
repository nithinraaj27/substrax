package com.substrax.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.nio.file.Path;

@Data
@AllArgsConstructor
public class LedgerBatchMetadata {
    private String batchId;
    private Path dataFile;
    private int recordCount;
    private BigDecimal totalAmount;
    private String currency;
    private String sha256;
}
