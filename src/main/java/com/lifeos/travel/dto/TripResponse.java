package com.lifeos.travel.dto;

import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private UUID id;
    private String destination;
    private String tripTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalBudget;
    private BigDecimal actualSpend;
    private String currency;
    private TripStatus status;
    private String notes;
    private String coverImageUrl;
    private int travelersCount;
    private int itineraryItemsCount;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TripResponse fromEntity(TripEntity entity, int travelersCount, int itineraryItemsCount) {
        return TripResponse.builder()
                .id(entity.getId())
                .destination(entity.getDestination())
                .tripTitle(entity.getTripTitle())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalBudget(entity.getTotalBudget())
                .actualSpend(entity.getActualSpend())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .coverImageUrl(entity.getCoverImageUrl())
                .travelersCount(travelersCount)
                .itineraryItemsCount(itineraryItemsCount)
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
