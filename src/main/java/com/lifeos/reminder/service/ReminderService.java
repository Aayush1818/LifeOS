package com.lifeos.reminder.service;

import com.lifeos.reminder.dto.CreateReminderRequest;
import com.lifeos.reminder.dto.ReminderResponse;
import com.lifeos.reminder.dto.UpdateReminderRequest;
import com.lifeos.reminder.entity.ReminderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReminderService {

    Page<ReminderResponse> getReminders(UUID userId, ReminderStatus status, Pageable pageable);

    List<ReminderResponse> getUpcomingReminders(UUID userId, int windowDays);

    List<ReminderResponse> getOverdueReminders(UUID userId);

    ReminderResponse getReminderById(UUID userId, UUID reminderId);

    ReminderResponse createReminder(UUID userId, CreateReminderRequest request);

    ReminderResponse updateReminder(UUID userId, UUID reminderId, UpdateReminderRequest request);

    ReminderResponse completeReminder(UUID userId, UUID reminderId);

    ReminderResponse dismissReminder(UUID userId, UUID reminderId);

    void deleteReminder(UUID userId, UUID reminderId);
}
