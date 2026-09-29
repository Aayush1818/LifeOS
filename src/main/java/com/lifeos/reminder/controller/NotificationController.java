package com.lifeos.reminder.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.reminder.dto.NotificationCountDto;
import com.lifeos.reminder.dto.NotificationResponse;
import com.lifeos.reminder.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "In-App Notifications", description = "In-app alerts, deadline triggers, and notification center")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications", description = "Retrieves paginated notifications for the authenticated user.")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> listNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<NotificationResponse> response = notificationService.getNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Fast query returning the total count of unread notifications for UI badges.")
    public ResponseEntity<ApiResponse<NotificationCountDto>> getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationCountDto response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Unread count retrieved successfully"));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationResponse response = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Bulk marks all unread notifications as read for the authenticated tenant.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        int updated = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("markedReadCount", updated), "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dismiss/delete notification", description = "Soft-deletes a notification.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification deleted successfully"));
    }
}
