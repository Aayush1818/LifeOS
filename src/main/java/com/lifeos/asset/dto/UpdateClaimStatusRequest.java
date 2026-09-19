package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClaimStatusRequest {

    @NotNull(message = "Claim status is required")
    private ClaimStatus status;

    private String resolution;
    private LocalDate resolvedDate;

    @PositiveOrZero(message = "Covered cost must be positive or zero")
    private BigDecimal claimCostCovered;

    @PositiveOrZero(message = "Out of pocket cost must be positive or zero")
    private BigDecimal outOfPocketCost;
}
