package com.substrax.notificationservice.repository;

import com.substrax.notificationservice.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEvent, UUID> {

    Optional<NotificationEvent> findByEventId(String eventId);

    List<NotificationEvent> findByUserIdOrderByCreatedAtDesc(String userId);
}
