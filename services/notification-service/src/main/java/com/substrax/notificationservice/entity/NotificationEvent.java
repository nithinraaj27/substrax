package com.substrax.notificationservice.entity;

import com.substrax.notificationservice.dto.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "notification_event",
        indexes = {
                @Index(name="idx_notification_user", columnList = "userId"),
                @Index(name="idx_notification_event", columnList = "eventId", unique = true)
        }
)
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, unique = true)
    private String eventId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
        this.status = NotificationStatus.UNREAD.name();
    }
}
