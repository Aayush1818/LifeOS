package com.lifeos.travel.dto;

import com.lifeos.travel.entity.BookingStatus;
import com.lifeos.travel.entity.ItineraryItemType;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItineraryItemRequest {

    private ItineraryItemType itemType;

    @Size(max = 100, message = "Custom type name cannot exceed 100 characters")
    private String customTypeName;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 150, message = "Provider cannot exceed 150 characters")
    private String provider;

    @Size(max = 100, message = "Booking reference cannot exceed 100 characters")
    private String bookingReference;

    private String confirmationDetails;

    private OffsetDateTime startTime;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String startTimeZone;

    @Size(max = 255, message = "Start location cannot exceed 255 characters")
    private String startLocation;

    private OffsetDateTime endTime;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String endTimeZone;

    @Size(max = 255, message = "End location cannot exceed 255 characters")
    private String endLocation;

    private BookingStatus status;

    @PositiveOrZero(message = "Cost must be positive or zero")
    private BigDecimal cost;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    @Positive(message = "Exchange rate must be positive")
    private BigDecimal exchangeRateToBase;

    @Positive(message = "Reminder offset minutes must be positive")
    private Integer reminderOffsetMinutes;

    private String notes;

    private Map<String, Object> metadata;
}
