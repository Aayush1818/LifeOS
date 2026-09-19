package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ClaimType;
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
public class UpdateWarrantyClaimRequest {

    private String claimNumber;
    private LocalDate claimDate;
    private ClaimType claimType;
    private String description;
    private String resolution;
    private LocalDate resolvedDate;

    @PositiveOrZero(message = "Covered cost must be positive or zero")
    private BigDecimal claimCostCovered;

    @PositiveOrZero(message = "Out of pocket cost must be positive or zero")
    private BigDecimal outOfPocketCost;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;
}
