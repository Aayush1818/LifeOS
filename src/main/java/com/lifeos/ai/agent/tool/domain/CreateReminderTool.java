package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Mutating tool for creating reminders.
 * Requires human-in-the-loop confirmation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateReminderTool implements LifeOSTool {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    @Override
    public String getName() {
        return "create_reminder";
    }

    @Override
    public String getDescription() {
        return "Creates a new scheduled personal reminder with title, due date/time, and optional description.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("title")
                        .type("string")
                        .description("Title of the reminder (e.g., 'Pay electricity bill', 'Dentist follow-up')")
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("dueAt")
                        .type("string")
                        .description("ISO-8601 timestamp when the reminder is due (e.g., '2026-09-25T09:00:00Z')")
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("description")
                        .type("string")
                        .description("Optional additional notes or context")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return true;
    }

    @Override
    public String getConfirmationPrompt(Map<String, Object> parameters) {
        String title = parameters.getOrDefault("title", "reminder").toString();
        String dueAt = parameters.getOrDefault("dueAt", "the specified time").toString();
        return "Would you like me to schedule a reminder for '" + title + "' due on " + dueAt + "?";
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.info("Executing CreateReminderTool for user [{}] with params: {}", userId, parameters);
        try {
            if (!parameters.containsKey("title") || parameters.get("title") == null) {
                return ToolExecutionResult.failure("Parameter 'title' is required");
            }
            if (!parameters.containsKey("dueAt") || parameters.get("dueAt") == null) {
                return ToolExecutionResult.failure("Parameter 'dueAt' is required");
            }

            String title = parameters.get("title").toString().trim();
            OffsetDateTime dueAt = OffsetDateTime.parse(parameters.get("dueAt").toString().trim());
            String description = parameters.containsKey("description") && parameters.get("description") != null
                    ? parameters.get("description").toString().trim()
                    : "";

            UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

            ReminderEntity reminder = ReminderEntity.builder()
                    .user(user)
                    .title(title)
                    .description(description)
                    .dueAt(dueAt)
                    .reminderType("PERSONAL")
                    .status(ReminderStatus.ACTIVE)
                    .build();

            ReminderEntity saved = reminderRepository.save(reminder);
            return ToolExecutionResult.success(Map.of(
                    "reminderId", saved.getId().toString(),
                    "title", saved.getTitle(),
                    "dueAt", saved.getDueAt().toString(),
                    "status", saved.getStatus().name()
            ));
        } catch (Exception e) {
            log.error("Failed to create reminder for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to create reminder: " + e.getMessage());
        }
    }
}
