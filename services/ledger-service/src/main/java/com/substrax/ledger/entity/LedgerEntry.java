package com.substrax.ledger.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false)
    private String eventId;

    @Column(nullable = false)
    private String ledgerId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String reference;

    @Column(nullable = false)
    private Long eventTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "raw_event", columnDefinition = "jsonb", nullable = false)
    @org.hibernate.annotations.ColumnTransformer(
            write = "?::jsonb"
    )
    private String rawEvent;

    @Column(nullable = false)
    private boolean exported = false;

    @Column
    private Instant exportedAt;

    @Column(length = 64)
    private String batchId;
}
