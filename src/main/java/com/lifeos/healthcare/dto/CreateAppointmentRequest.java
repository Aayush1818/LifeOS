package com.lifeos.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotBlank(message = "Doctor name is required")
    @Size(max = 150, message = "Doctor name cannot exceed 150 characters")
    private String doctorName;

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;

    @NotBlank(message = "Clinic or hospital name is required")
    @Size(max = 200, message = "Clinic or hospital cannot exceed 200 characters")
    private String clinicOrHospital;

    @Size(max = 30, message = "Clinic phone cannot exceed 30 characters")
    private String clinicPhone;

    @Size(max = 255, message = "Clinic address cannot exceed 255 characters")
    private String clinicAddress;

    @NotNull(message = "Appointment time is required")
    private OffsetDateTime appointmentTime;

    private OffsetDateTime scheduledEndTime;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timeZone;

    @NotBlank(message = "Appointment purpose is required")
    @Size(max = 255, message = "Purpose cannot exceed 255 characters")
    private String purpose;

    private String notes;

    private UUID dependentId;

    private UUID primaryDocumentId;

    private UUID followUpToId;

    @Positive(message = "Reminder offset minutes must be positive")
    private Integer reminderOffsetMinutes;

    private Map<String, Object> metadata;
}
