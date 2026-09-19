package com.lifeos.insurance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewPolicyRequest {

    @NotNull(message = "New expiry date is required")
    private LocalDate newExpiryDate;

    private LocalDate newRenewalDate;

    @DecimalMin(value = "0.01", message = "New premium amount must be strictly positive")
    private BigDecimal newPremiumAmount;

    private UUID documentId;

    private String notes;
}
