package com.lifeos.healthcare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class RescheduleAppointmentRequest {

    @NotNull(message = "New appointment time is required")
    private OffsetDateTime newAppointmentTime;

    private OffsetDateTime newScheduledEndTime;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timeZone;

    private String notes;

    @Positive(message = "Reminder offset minutes must be positive")
    private Integer reminderOffsetMinutes;
}
