package com.lifeos.dependent.dto;

import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.entity.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependentResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private RelationshipType relationship;
    private LocalDate dateOfBirth;
    private String emergencyPhone;
    private Map<String, Object> medicalNotes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static DependentResponse fromEntity(DependentEntity entity) {
        return DependentResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .fullName(entity.getFullName())
                .relationship(entity.getRelationship())
                .dateOfBirth(entity.getDateOfBirth())
                .emergencyPhone(entity.getEmergencyPhone())
                .medicalNotes(entity.getMedicalNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
