package com.lifeos.reminder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.reminder.dto.NotificationCountDto;
import com.lifeos.reminder.dto.NotificationResponse;
import com.lifeos.reminder.entity.NotificationChannel;
import com.lifeos.reminder.entity.NotificationEntity;
import com.lifeos.reminder.entity.NotificationType;
import com.lifeos.reminder.repository.NotificationRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResponse dispatchNotification(
            UUID userId,
            String title,
            String message,
            NotificationChannel channel,
            NotificationType type,
            UUID targetEntityId,
            Map<String, Object> metadata
    ) {
        log.info("Dispatching notification [{}] for user [{}] type [{}]", title, userId, type);

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String metadataJson = "{}";
        if (metadata != null && !metadata.isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                log.warn("Failed to serialize metadata for notification: {}", e.getMessage());
            }
        }

        NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .title(title)
                .message(message)
                .channel(channel != null ? channel : NotificationChannel.IN_APP)
                .notificationType(type)
                .targetEntityId(targetEntityId)
                .metadata(metadataJson)
                .isRead(false)
                .build();

        NotificationEntity saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<NotificationEntity> page = unreadOnly
                ? notificationRepository.findAllByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

        return page.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountDto getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
        return new NotificationCountDto(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsReadByUserId(userId, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void deleteNotification(UUID userId, UUID notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserIdAndIsDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setDeleted(true);
        notificationRepository.save(notification);
    }
}
