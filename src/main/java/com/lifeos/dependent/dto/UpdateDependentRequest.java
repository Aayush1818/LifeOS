package com.lifeos.dependent.dto;

import com.lifeos.dependent.entity.RelationshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDependentRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @NotNull(message = "Relationship is required")
    private RelationshipType relationship;

    private LocalDate dateOfBirth;

    @Size(max = 30, message = "Emergency phone must not exceed 30 characters")
    private String emergencyPhone;

    private Map<String, Object> medicalNotes;
}
