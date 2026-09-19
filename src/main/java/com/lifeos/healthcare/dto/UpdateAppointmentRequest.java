package com.lifeos.healthcare.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentRequest {

    @Size(max = 150, message = "Doctor name cannot exceed 150 characters")
    private String doctorName;

    @Size(max = 100, message = "Specialization cannot exceed 100 characters")
    private String specialization;

    @Size(max = 200, message = "Clinic or hospital cannot exceed 200 characters")
    private String clinicOrHospital;

    @Size(max = 30, message = "Clinic phone cannot exceed 30 characters")
    private String clinicPhone;

    @Size(max = 255, message = "Clinic address cannot exceed 255 characters")
    private String clinicAddress;

    @Size(max = 255, message = "Purpose cannot exceed 255 characters")
    private String purpose;

    private String notes;

    private UUID primaryDocumentId;

    private Map<String, Object> metadata;
}
