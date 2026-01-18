package com.substrax.notificationservice.implementaion;

import com.substrax.notificationservice.dto.NotificationStatus;
import com.substrax.notificationservice.entity.NotificationEvent;
import com.substrax.notificationservice.repository.NotificationRepository;
import com.substrax.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;

    @Transactional
    @Override
    public void createNotification(String eventId, String userId, String type, String message) {

        if(repository.findByEventId(eventId).isPresent()){
            log.info("Notification already exists for eventId={} , skipping", eventId);
            return;
        }

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(userId)
                .type(type)
                .message(message)
                .status(NotificationStatus.UNREAD.name())
                .build();

        repository.save(notificationEvent);

        log.info("Notification created for userId={} type={}", userId, type);
    }

    @Override
    public List<NotificationEvent> getUserNotification(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAsRead(String eventId) {
        repository.findByEventId(eventId).ifPresent(notificationEvent -> {
            notificationEvent.setStatus(NotificationStatus.READ.name());
            repository.save(notificationEvent);
        });
    }
}
