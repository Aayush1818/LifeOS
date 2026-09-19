package com.lifeos.travel.dto;

import com.lifeos.travel.entity.TripTravelerEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripTravelerResponse {

    private UUID id;
    private String travelerName;
    private UUID dependentId;
    private String dependentName;
    private boolean isPrimaryUser;
    private String notes;
    private OffsetDateTime createdAt;

    public static TripTravelerResponse fromEntity(TripTravelerEntity entity) {
        return TripTravelerResponse.builder()
                .id(entity.getId())
                .travelerName(entity.getTravelerName())
                .dependentId(entity.getDependent() != null ? entity.getDependent().getId() : null)
                .dependentName(entity.getDependent() != null ? entity.getDependent().getFullName() : null)
                .isPrimaryUser(entity.isPrimaryUser())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
