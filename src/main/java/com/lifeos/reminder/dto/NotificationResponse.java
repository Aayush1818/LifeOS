package com.lifeos.reminder.dto;

import com.lifeos.reminder.entity.NotificationChannel;
import com.lifeos.reminder.entity.NotificationEntity;
import com.lifeos.reminder.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private boolean isRead;
    private OffsetDateTime readAt;
    private UUID targetEntityId;
    private String metadata;
    private OffsetDateTime createdAt;

    public static NotificationResponse fromEntity(NotificationEntity entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .title(entity.getTitle())
                .message(entity.getMessage())
                .channel(entity.getChannel())
                .notificationType(entity.getNotificationType())
                .isRead(entity.isRead())
                .readAt(entity.getReadAt())
                .targetEntityId(entity.getTargetEntityId())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
