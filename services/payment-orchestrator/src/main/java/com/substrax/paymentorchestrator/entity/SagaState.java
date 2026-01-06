package com.substrax.paymentorchestrator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_state",
        indexes = {
            @Index(name = "idx_saga_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_saga_current_state", columnList = "current_state")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @Column(name="saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "current_state", nullable = false, length = 30)
    private String currentState;

    @Column(name = "last_event", length = 30)
    private String lastEvent;

    @Column(name = "compensation_required", nullable = false)
    private boolean compensationRequired;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name= "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }
}
