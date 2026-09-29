package com.lifeos.reminder.dto;

import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
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
public class ReminderResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private OffsetDateTime dueAt;
    private String recurrencePattern;
    private String reminderType;
    private ReminderStatus status;
    private UUID targetEntityId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ReminderResponse fromEntity(ReminderEntity entity) {
        if (entity == null) return null;
        return ReminderResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .dueAt(entity.getDueAt())
                .recurrencePattern(entity.getRecurrencePattern())
                .reminderType(entity.getReminderType())
                .status(entity.getStatus())
                .targetEntityId(entity.getTargetEntityId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
