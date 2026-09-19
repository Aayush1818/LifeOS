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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDetailResponse {

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

    @Builder.Default
    private List<TripTravelerResponse> travelers = new ArrayList<>();

    @Builder.Default
    private List<ItineraryItemResponse> itinerary = new ArrayList<>();

    private TripSpendSummary spendSummary;

    @Builder.Default
    private List<TravelDocumentLinkResponse> linkedDocuments = new ArrayList<>();

    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TripDetailResponse fromEntity(
            TripEntity entity,
            List<TripTravelerResponse> travelers,
            List<ItineraryItemResponse> itinerary,
            TripSpendSummary spendSummary,
            List<TravelDocumentLinkResponse> linkedDocuments
    ) {
        return TripDetailResponse.builder()
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
                .travelers(travelers != null ? travelers : new ArrayList<>())
                .itinerary(itinerary != null ? itinerary : new ArrayList<>())
                .spendSummary(spendSummary)
                .linkedDocuments(linkedDocuments != null ? linkedDocuments : new ArrayList<>())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
