package com.lifeos.reminder.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateReminderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Due date/time is required")
    @JsonAlias({"dueDate", "due_date", "due_at"})
    private OffsetDateTime dueAt;

    @JsonAlias({"recurrenceRule", "recurrence_rule", "recurrence_pattern"})
    private String recurrencePattern;

    @Builder.Default
    private String reminderType = "PERSONAL";

    private UUID targetEntityId;
}
