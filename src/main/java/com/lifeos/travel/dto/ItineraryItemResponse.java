package com.lifeos.travel.dto;

import com.lifeos.travel.entity.BookingStatus;
import com.lifeos.travel.entity.ItineraryItemEntity;
import com.lifeos.travel.entity.ItineraryItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryItemResponse {

    private UUID id;
    private UUID tripId;
    private ItineraryItemType itemType;
    private String customTypeName;
    private String title;
    private String provider;
    private String bookingReference;
    private String confirmationDetails;

    private OffsetDateTime startTime;
    private String startTimeZone;
    private String startLocation;

    private OffsetDateTime endTime;
    private String endTimeZone;
    private String endLocation;

    private BookingStatus status;
    private BigDecimal cost;
    private String currency;
    private BigDecimal exchangeRateToBase;
    private Integer reminderOffsetMinutes;
    private String notes;
    private Map<String, Object> metadata;

    private boolean overnightOrCrossDate;

    @Builder.Default
    private List<TravelDocumentLinkResponse> linkedDocuments = new ArrayList<>();

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ItineraryItemResponse fromEntity(ItineraryItemEntity entity, List<TravelDocumentLinkResponse> docs) {
        boolean crossDate = false;
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            try {
                ZoneId startZone = ZoneId.of(entity.getStartTimeZone());
                ZoneId endZone = ZoneId.of(entity.getEndTimeZone());
                crossDate = entity.getEndTime().atZoneSameInstant(endZone).toLocalDate()
                        .isAfter(entity.getStartTime().atZoneSameInstant(startZone).toLocalDate());
            } catch (Exception ignored) {
                crossDate = entity.getEndTime().toLocalDate().isAfter(entity.getStartTime().toLocalDate());
            }
        }

        return ItineraryItemResponse.builder()
                .id(entity.getId())
                .tripId(entity.getTrip().getId())
                .itemType(entity.getItemType())
                .customTypeName(entity.getCustomTypeName())
                .title(entity.getTitle())
                .provider(entity.getProvider())
                .bookingReference(entity.getBookingReference())
                .confirmationDetails(entity.getConfirmationDetails())
                .startTime(entity.getStartTime())
                .startTimeZone(entity.getStartTimeZone())
                .startLocation(entity.getStartLocation())
                .endTime(entity.getEndTime())
                .endTimeZone(entity.getEndTimeZone())
                .endLocation(entity.getEndLocation())
                .status(entity.getStatus())
                .cost(entity.getCost())
                .currency(entity.getCurrency())
                .exchangeRateToBase(entity.getExchangeRateToBase())
                .reminderOffsetMinutes(entity.getReminderOffsetMinutes())
                .notes(entity.getNotes())
                .metadata(entity.getMetadata())
                .overnightOrCrossDate(crossDate)
                .linkedDocuments(docs != null ? docs : new ArrayList<>())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ItineraryItemResponse fromEntity(ItineraryItemEntity entity) {
        return fromEntity(entity, new ArrayList<>());
    }
}
