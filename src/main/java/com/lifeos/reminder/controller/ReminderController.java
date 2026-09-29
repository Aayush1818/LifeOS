package com.lifeos.reminder.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.reminder.dto.CreateReminderRequest;
import com.lifeos.reminder.dto.ReminderResponse;
import com.lifeos.reminder.dto.UpdateReminderRequest;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Reminders & Deadlines", description = "Personal reminders, recurrence rules, and domain deadline tracking")
public class ReminderController {

    private final ReminderService reminderService;

    @PostMapping
    @Operation(summary = "Create reminder", description = "Creates a new personal reminder with optional recurrence.")
    public ResponseEntity<ApiResponse<ReminderResponse>> createReminder(
            @Valid @RequestBody CreateReminderRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReminderResponse response = reminderService.createReminder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Reminder created successfully"));
    }

    @GetMapping
    @Operation(summary = "List reminders", description = "Retrieves paginated reminders belonging to the authenticated tenant.")
    public ResponseEntity<ApiResponse<Page<ReminderResponse>>> listReminders(
            @RequestParam(required = false) ReminderStatus status,
            @PageableDefault(size = 20, sort = "dueAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<ReminderResponse> response = reminderService.getReminders(userId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminders retrieved successfully"));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming reminders", description = "Returns active reminders scheduled within the next window of days.")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getUpcomingReminders(
            @RequestParam(defaultValue = "14") int windowDays
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<ReminderResponse> response = reminderService.getUpcomingReminders(userId, windowDays);
        return ResponseEntity.ok(ApiResponse.success(response, "Upcoming reminders retrieved successfully"));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue reminders", description = "Returns active reminders with past due date.")
    public ResponseEntity<ApiResponse<List<ReminderResponse>>> getOverdueReminders() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<ReminderResponse> response = reminderService.getOverdueReminders(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Overdue reminders retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reminder details", description = "Fetches a single reminder with tenant isolation.")
    public ResponseEntity<ApiResponse<ReminderResponse>> getReminder(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReminderResponse response = reminderService.getReminderById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update reminder", description = "Updates reminder title, due date, description or recurrence.")
    public ResponseEntity<ApiResponse<ReminderResponse>> updateReminder(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReminderRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReminderResponse response = reminderService.updateReminder(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder updated successfully"));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark reminder complete", description = "Marks reminder as COMPLETED and automatically schedules next occurrence if recurring.")
    public ResponseEntity<ApiResponse<ReminderResponse>> completeReminder(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReminderResponse response = reminderService.completeReminder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder marked as completed"));
    }

    @PatchMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss reminder", description = "Transitions reminder status to DISMISSED.")
    public ResponseEntity<ApiResponse<ReminderResponse>> dismissReminder(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReminderResponse response = reminderService.dismissReminder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reminder dismissed"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete reminder", description = "Soft-deletes the reminder.")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        reminderService.deleteReminder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Reminder deleted successfully"));
    }
}
