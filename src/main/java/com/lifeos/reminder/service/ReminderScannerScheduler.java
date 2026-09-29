package com.lifeos.reminder.service;

import com.lifeos.reminder.entity.NotificationChannel;
import com.lifeos.reminder.entity.NotificationType;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.NotificationRepository;
import com.lifeos.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScannerScheduler {

    private final ReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Periodic background scan executing every 15 minutes by default.
     * Scans for due and upcoming active reminders across all LifeOS domains.
     */
    @Scheduled(cron = "${lifeos.reminders.scanner.cron:0 */15 * * * *}")
    @Transactional
    public void runPeriodicScanner() {
        log.info("Starting background reminder and deadline scanner...");
        int processed = scanAndDispatchDueReminders();
        log.info("Finished background scanner. Dispatched notifications for [{}] reminder(s).", processed);
    }

    /**
     * Scans for active reminders that have reached their due date and triggers notifications.
     */
    @Transactional
    public int scanAndDispatchDueReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ReminderEntity> dueReminders = reminderRepository
                .findAllByStatusAndDueAtLessThanEqualAndIsDeletedFalse(ReminderStatus.ACTIVE, now);

        int count = 0;
        OffsetDateTime cooldownCutoff = now.minusHours(24);

        for (ReminderEntity reminder : dueReminders) {
            try {
                NotificationType notifType = mapReminderType(reminder.getReminderType());

                // Guard against duplicate notification dispatch within 24h
                boolean alreadyNotified = notificationRepository
                        .existsByTargetEntityIdAndNotificationTypeAndCreatedAtAfterAndIsDeletedFalse(
                                reminder.getId(), notifType, cooldownCutoff
                        );

                if (!alreadyNotified && reminder.getUser() != null) {
                    notificationService.dispatchNotification(
                            reminder.getUser().getId(),
                            reminder.getTitle(),
                            reminder.getDescription() != null && !reminder.getDescription().isBlank()
                                    ? reminder.getDescription()
                                    : "Reminder due: " + reminder.getTitle(),
                            NotificationChannel.IN_APP,
                            notifType,
                            reminder.getId(),
                            Map.of(
                                    "reminderId", reminder.getId().toString(),
                                    "reminderType", reminder.getReminderType(),
                                    "dueAt", reminder.getDueAt().toString()
                            )
                    );
                    count++;
                }
            } catch (Exception e) {
                log.error("Failed to process reminder [{}]: {}", reminder.getId(), e.getMessage());
            }
        }

        return count;
    }

    private NotificationType mapReminderType(String reminderType) {
        if (reminderType == null) return NotificationType.REMINDER_DUE;
        return switch (reminderType.toUpperCase()) {
            case "INSURANCE_RENEWAL" -> NotificationType.INSURANCE_EXPIRY;
            case "HEALTH_APPOINTMENT" -> NotificationType.APPOINTMENT_ALERT;
            case "WARRANTY_EXPIRY" -> NotificationType.WARRANTY_EXPIRY;
            case "LOAN_EMI" -> NotificationType.LOAN_EMI;
            default -> NotificationType.REMINDER_DUE;
        };
    }
}
