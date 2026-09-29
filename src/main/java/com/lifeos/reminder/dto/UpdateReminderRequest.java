package com.lifeos.reminder.dto;

import com.lifeos.reminder.entity.ReminderStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReminderRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    private OffsetDateTime dueAt;

    private String recurrencePattern;

    private ReminderStatus status;
}
