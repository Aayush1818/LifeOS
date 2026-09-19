package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ClaimType;
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
public class CreateWarrantyClaimRequest {

    private String claimNumber;

    @NotNull(message = "Claim date is required")
    private LocalDate claimDate;

    @NotNull(message = "Claim type is required")
    private ClaimType claimType;

    @NotBlank(message = "Description is required")
    private String description;

    private String resolution;
    private LocalDate resolvedDate;

    @PositiveOrZero(message = "Covered cost must be positive or zero")
    private BigDecimal claimCostCovered;

    @PositiveOrZero(message = "Out of pocket cost must be positive or zero")
    private BigDecimal outOfPocketCost;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";
}
