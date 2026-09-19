package com.lifeos.asset.dto;

import com.lifeos.asset.entity.WarrantyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarrantyRequest {

    @NotBlank(message = "Warranty provider is required")
    private String provider;

    @NotNull(message = "Warranty type is required")
    private WarrantyType warrantyType;

    private String policyNumber;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate expiryDate;

    private String coverageDetails;

    @PositiveOrZero(message = "Deductible amount must be positive or zero")
    private BigDecimal deductibleAmount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private Integer reminderOffsetDays = 30;

    private String contactPhone;
    private String contactEmail;
    private String notes;
}
