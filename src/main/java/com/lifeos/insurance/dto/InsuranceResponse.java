package com.lifeos.insurance.dto;

import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.entity.PremiumFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceResponse {

    private UUID id;
    private String policyNumber;
    private String policyName;
    private String providerName;
    private InsuranceType policyType;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private PremiumFrequency premiumFrequency;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private LocalDate nextRenewalDate;
    private PolicyStatus status;
    private UUID dependentId;
    private String dependentName;
    private UUID documentId;
    private String documentTitle;
    private String notes;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static InsuranceResponse fromEntity(InsurancePolicyEntity entity) {
        return InsuranceResponse.builder()
                .id(entity.getId())
                .policyNumber(entity.getPolicyNumber())
                .policyName(entity.getPolicyName())
                .providerName(entity.getProviderName())
                .policyType(entity.getPolicyType())
                .coverageAmount(entity.getCoverageAmount())
                .premiumAmount(entity.getPremiumAmount())
                .premiumFrequency(entity.getPremiumFrequency())
                .startDate(entity.getStartDate())
                .expiryDate(entity.getExpiryDate())
                .nextRenewalDate(entity.getNextRenewalDate())
                .status(entity.getStatus())
                .dependentId(entity.getDependent() != null ? entity.getDependent().getId() : null)
                .dependentName(entity.getDependent() != null ? entity.getDependent().getFullName() : null)
                .documentId(entity.getDocument() != null ? entity.getDocument().getId() : null)
                .documentTitle(entity.getDocument() != null ? entity.getDocument().getTitle() : null)
                .notes(entity.getNotes())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
