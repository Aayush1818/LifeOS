package com.lifeos.reminder.service;

import com.lifeos.reminder.dto.NotificationCountDto;
import com.lifeos.reminder.dto.NotificationResponse;
import com.lifeos.reminder.entity.NotificationChannel;
import com.lifeos.reminder.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse dispatchNotification(
            UUID userId,
            String title,
            String message,
            NotificationChannel channel,
            NotificationType type,
            UUID targetEntityId,
            Map<String, Object> metadata
    );

    Page<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable);

    NotificationCountDto getUnreadCount(UUID userId);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    int markAllAsRead(UUID userId);

    void deleteNotification(UUID userId, UUID notificationId);
}
