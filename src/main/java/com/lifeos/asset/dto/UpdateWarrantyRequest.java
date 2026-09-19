package com.lifeos.asset.dto;

import com.lifeos.asset.entity.WarrantyType;
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
public class UpdateWarrantyRequest {

    private String provider;
    private WarrantyType warrantyType;
    private String policyNumber;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private String coverageDetails;

    @PositiveOrZero(message = "Deductible amount must be positive or zero")
    private BigDecimal deductibleAmount;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private Integer reminderOffsetDays;
    private String contactPhone;
    private String contactEmail;
    private String notes;
}
