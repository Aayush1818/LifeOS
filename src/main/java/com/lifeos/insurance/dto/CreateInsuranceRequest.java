package com.lifeos.insurance.dto;

import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PremiumFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInsuranceRequest {

    @NotBlank(message = "Policy number is required")
    @Size(max = 100, message = "Policy number must not exceed 100 characters")
    private String policyNumber;

    @Size(max = 200, message = "Policy name must not exceed 200 characters")
    private String policyName;

    @NotBlank(message = "Provider name is required")
    @Size(max = 150, message = "Provider name must not exceed 150 characters")
    private String providerName;

    @NotNull(message = "Policy type is required")
    private InsuranceType policyType;

    @NotNull(message = "Coverage amount is required")
    @DecimalMin(value = "0.01", message = "Coverage amount must be strictly positive")
    private BigDecimal coverageAmount;

    @NotNull(message = "Premium amount is required")
    @DecimalMin(value = "0.01", message = "Premium amount must be strictly positive")
    private BigDecimal premiumAmount;

    @NotNull(message = "Premium frequency is required")
    private PremiumFrequency premiumFrequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    private LocalDate nextRenewalDate;

    private UUID dependentId;

    private UUID documentId;

    private String notes;

    private Map<String, Object> metadata;
}
