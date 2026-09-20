package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.healthcare.dto.AppointmentResponse;
import com.lifeos.healthcare.entity.AppointmentStatus;
import com.lifeos.healthcare.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for retrieving user's upcoming doctor visits and healthcare consultations.
 * Strictly non-diagnostic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthcareAppointmentsTool implements LifeOSTool {

    private final AppointmentService appointmentService;

    @Override
    public String getName() {
        return "get_upcoming_appointments";
    }

    @Override
    public String getDescription() {
        return "Retrieves user's upcoming scheduled healthcare appointments, doctors, clinics, and consultation times. Strictly administrative and non-diagnostic.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("windowDays")
                        .type("integer")
                        .description("Window in days to search for upcoming appointments (default 30)")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing HealthcareAppointmentsTool for user [{}]", userId);
        try {
            int windowDays = 30;
            if (parameters != null && parameters.containsKey("windowDays") && parameters.get("windowDays") != null) {
                try {
                    windowDays = Integer.parseInt(parameters.get("windowDays").toString());
                } catch (NumberFormatException ignored) {}
            }

            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(windowDays);

            Page<AppointmentResponse> page = appointmentService.listAppointments(
                    userId, null, AppointmentStatus.SCHEDULED, start, end, null, PageRequest.of(0, 50));

            return ToolExecutionResult.success(page.getContent());
        } catch (Exception e) {
            log.error("Failed to fetch appointments for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to fetch appointments: " + e.getMessage());
        }
    }
}
