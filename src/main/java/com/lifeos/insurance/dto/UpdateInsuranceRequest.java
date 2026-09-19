package com.lifeos.insurance.dto;

import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.entity.PremiumFrequency;
import jakarta.validation.constraints.DecimalMin;
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
public class UpdateInsuranceRequest {

    @Size(max = 200, message = "Policy name must not exceed 200 characters")
    private String policyName;

    @Size(max = 150, message = "Provider name must not exceed 150 characters")
    private String providerName;

    private InsuranceType policyType;

    @DecimalMin(value = "0.01", message = "Coverage amount must be strictly positive")
    private BigDecimal coverageAmount;

    @DecimalMin(value = "0.01", message = "Premium amount must be strictly positive")
    private BigDecimal premiumAmount;

    private PremiumFrequency premiumFrequency;

    private LocalDate expiryDate;

    private LocalDate nextRenewalDate;

    private PolicyStatus status;

    private UUID dependentId;

    private UUID documentId;

    private String notes;

    private Map<String, Object> metadata;
}
