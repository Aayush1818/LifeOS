package com.lifeos.reminder.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.reminder.dto.CreateReminderRequest;
import com.lifeos.reminder.dto.ReminderResponse;
import com.lifeos.reminder.dto.UpdateReminderRequest;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReminderService implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderResponse> getReminders(UUID userId, ReminderStatus status, Pageable pageable) {
        Page<ReminderEntity> page = status != null
                ? reminderRepository.findAllByUserIdAndStatusAndIsDeletedFalseOrderByDueAtAsc(userId, status, pageable)
                : reminderRepository.findAllByUserIdAndIsDeletedFalseOrderByDueAtAsc(userId, pageable);

        return page.map(ReminderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> getUpcomingReminders(UUID userId, int windowDays) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plusDays(windowDays > 0 ? windowDays : 14);

        List<ReminderEntity> reminders = reminderRepository
                .findAllByUserIdAndDueAtBetweenAndStatusAndIsDeletedFalseOrderByDueAtAsc(
                        userId, now, windowEnd, ReminderStatus.ACTIVE
                );

        return reminders.stream().map(ReminderResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> getOverdueReminders(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<ReminderEntity> reminders = reminderRepository
                .findAllByUserIdAndDueAtBeforeAndStatusAndIsDeletedFalseOrderByDueAtAsc(
                        userId, now, ReminderStatus.ACTIVE
                );
        return reminders.stream().map(ReminderResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderResponse getReminderById(UUID userId, UUID reminderId) {
        ReminderEntity reminder = reminderRepository.findByIdAndUserIdAndIsDeletedFalse(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        return ReminderResponse.fromEntity(reminder);
    }

    @Override
    @Transactional
    public ReminderResponse createReminder(UUID userId, CreateReminderRequest request) {
        log.info("Creating reminder [{}] for user [{}]", request.getTitle(), userId);

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ReminderEntity reminder = ReminderEntity.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueAt(request.getDueAt())
                .recurrencePattern(request.getRecurrencePattern())
                .reminderType(request.getReminderType() != null ? request.getReminderType() : "PERSONAL")
                .status(ReminderStatus.ACTIVE)
                .targetEntityId(request.getTargetEntityId())
                .build();

        ReminderEntity saved = reminderRepository.save(reminder);
        return ReminderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ReminderResponse updateReminder(UUID userId, UUID reminderId, UpdateReminderRequest request) {
        ReminderEntity reminder = reminderRepository.findByIdAndUserIdAndIsDeletedFalse(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            reminder.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            reminder.setDescription(request.getDescription());
        }
        if (request.getDueAt() != null) {
            reminder.setDueAt(request.getDueAt());
        }
        if (request.getRecurrencePattern() != null) {
            reminder.setRecurrencePattern(request.getRecurrencePattern());
        }
        if (request.getStatus() != null) {
            reminder.setStatus(request.getStatus());
        }

        ReminderEntity updated = reminderRepository.save(reminder);
        return ReminderResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public ReminderResponse completeReminder(UUID userId, UUID reminderId) {
        ReminderEntity reminder = reminderRepository.findByIdAndUserIdAndIsDeletedFalse(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        reminder.setStatus(ReminderStatus.COMPLETED);
        ReminderEntity completed = reminderRepository.save(reminder);

        // Advance recurrence if configured
        if (reminder.getRecurrencePattern() != null && !reminder.getRecurrencePattern().isBlank()) {
            OffsetDateTime nextDue = computeNextDue(reminder.getDueAt(), reminder.getRecurrencePattern());
            if (nextDue != null) {
                ReminderEntity nextOccurrence = ReminderEntity.builder()
                        .user(reminder.getUser())
                        .title(reminder.getTitle())
                        .description(reminder.getDescription())
                        .dueAt(nextDue)
                        .recurrencePattern(reminder.getRecurrencePattern())
                        .reminderType(reminder.getReminderType())
                        .status(ReminderStatus.ACTIVE)
                        .targetEntityId(reminder.getTargetEntityId())
                        .build();
                reminderRepository.save(nextOccurrence);
                log.info("Scheduled recurring reminder [{}] next occurrence at [{}]", reminder.getTitle(), nextDue);
            }
        }

        return ReminderResponse.fromEntity(completed);
    }

    @Override
    @Transactional
    public ReminderResponse dismissReminder(UUID userId, UUID reminderId) {
        ReminderEntity reminder = reminderRepository.findByIdAndUserIdAndIsDeletedFalse(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        reminder.setStatus(ReminderStatus.DISMISSED);
        ReminderEntity dismissed = reminderRepository.save(reminder);
        return ReminderResponse.fromEntity(dismissed);
    }

    @Override
    @Transactional
    public void deleteReminder(UUID userId, UUID reminderId) {
        ReminderEntity reminder = reminderRepository.findByIdAndUserIdAndIsDeletedFalse(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found: " + reminderId));

        reminder.setDeleted(true);
        reminderRepository.save(reminder);
    }

    private OffsetDateTime computeNextDue(OffsetDateTime currentDue, String pattern) {
        if (pattern == null) return null;
        return switch (pattern.toUpperCase()) {
            case "DAILY" -> currentDue.plusDays(1);
            case "WEEKLY" -> currentDue.plusWeeks(1);
            case "MONTHLY" -> currentDue.plusMonths(1);
            case "YEARLY" -> currentDue.plusYears(1);
            default -> null;
        };
    }
}
