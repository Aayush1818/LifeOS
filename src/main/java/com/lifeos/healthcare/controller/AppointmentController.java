package com.lifeos.healthcare.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.healthcare.dto.*;
import com.lifeos.healthcare.entity.AppointmentStatus;
import com.lifeos.healthcare.service.AppointmentService;
import com.lifeos.healthcare.service.MedicalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/healthcare/appointments")
@RequiredArgsConstructor
@Tag(name = "Healthcare Appointments", description = "Doctor consultations, scheduling, and reminder synchronization (Non-Diagnostic)")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final MedicalDocumentService medicalDocumentService;

    @PostMapping
    @Operation(summary = "Schedule healthcare appointment", description = "Creates a new doctor or clinic consultation and synchronizes a reminder.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AppointmentResponse response = appointmentService.createAppointment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Healthcare appointment scheduled successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details", description = "Retrieves consultation details by ID. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AppointmentResponse response = appointmentService.getAppointment(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Appointment details retrieved successfully"));
    }

    @GetMapping
    @Operation(summary = "List appointments", description = "Retrieves paginated appointments for the authenticated user with optional filters.")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse>>> listAppointments(
            @RequestParam(required = false) UUID dependentId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String doctorName,
            @PageableDefault(size = 20, sort = "appointmentTime", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Page<AppointmentResponse> page = appointmentService.listAppointments(
                userId, dependentId, status, startDate, endDate, doctorName, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Appointments retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment", description = "Updates consultation details, doctor info, notes, or primary document.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AppointmentResponse response = appointmentService.updateAppointment(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Appointment updated successfully"));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule appointment", description = "Modifies appointment time and resynchronizes the linked due date reminder.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AppointmentResponse response = appointmentService.rescheduleAppointment(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Appointment rescheduled successfully"));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Update appointment status", description = "Transitions status (e.g. COMPLETED, CANCELLED) and manages reminder lifecycle.")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AppointmentResponse response = appointmentService.updateStatus(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Appointment status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete appointment", description = "Soft-deletes consultation and dismisses the linked reminder.")
    public ResponseEntity<ApiResponse<Void>> deleteAppointment(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        appointmentService.deleteAppointment(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Appointment deleted successfully"));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Upcoming appointments", description = "Retrieves appointments scheduled within the specified day window (default: 14 days).")
    public ResponseEntity<ApiResponse<UpcomingAppointmentsResponse>> getUpcomingAppointments(
            @RequestParam(defaultValue = "14") int windowDays,
            @RequestParam(required = false) UUID dependentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UpcomingAppointmentsResponse response = appointmentService.getUpcomingAppointments(userId, windowDays, dependentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Upcoming appointments retrieved successfully"));
    }

    @PostMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Attach document to appointment", description = "Attaches a prescription, lab report, or invoice to this appointment.")
    public ResponseEntity<ApiResponse<MedicalDocumentLinkResponse>> attachDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MedicalDocumentLinkResponse response = medicalDocumentService.attachDocument(id, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document attached to appointment successfully"));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    @Operation(summary = "Detach document from appointment", description = "Unlinks a document from this appointment.")
    public ResponseEntity<ApiResponse<Void>> detachDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        medicalDocumentService.detachDocument(id, documentId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document detached from appointment successfully"));
    }
}
