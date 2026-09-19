package com.lifeos.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {

    @NotBlank(message = "Destination is required")
    @Size(max = 200, message = "Destination cannot exceed 200 characters")
    private String destination;

    @NotBlank(message = "Trip title is required")
    @Size(max = 200, message = "Trip title cannot exceed 200 characters")
    private String tripTitle;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @PositiveOrZero(message = "Total budget must be positive or zero")
    private BigDecimal totalBudget;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";

    private String notes;

    @Size(max = 500, message = "Cover image URL cannot exceed 500 characters")
    private String coverImageUrl;

    private Map<String, Object> metadata;
}
