package com.substrax.notificationservice.service;

import com.substrax.notificationservice.entity.NotificationEvent;

import java.util.List;

public interface NotificationService {

    void createNotification(
            String eventId,
            String userId,
            String type,
            String message
    );

    List<NotificationEvent> getUserNotification(String userId);

    void markAsRead(String eventId);
}
