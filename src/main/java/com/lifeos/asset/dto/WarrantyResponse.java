package com.lifeos.asset.dto;

import com.lifeos.asset.entity.WarrantyEntity;
import com.lifeos.asset.entity.WarrantyStatus;
import com.lifeos.asset.entity.WarrantyType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyResponse {

    private UUID id;
    private UUID userId;
    private UUID assetId;
    private String assetName;
    private String provider;
    private WarrantyType warrantyType;
    private String policyNumber;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private WarrantyStatus status;
    private String coverageDetails;
    private BigDecimal deductibleAmount;
    private String currency;
    private UUID reminderId;
    private Integer reminderOffsetDays;
    private String contactPhone;
    private String contactEmail;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static WarrantyResponse fromEntity(WarrantyEntity entity) {
        if (entity == null) return null;
        return WarrantyResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .assetName(entity.getAsset() != null ? entity.getAsset().getName() : null)
                .provider(entity.getProvider())
                .warrantyType(entity.getWarrantyType())
                .policyNumber(entity.getPolicyNumber())
                .startDate(entity.getStartDate())
                .expiryDate(entity.getExpiryDate())
                .status(entity.getStatus())
                .coverageDetails(entity.getCoverageDetails())
                .deductibleAmount(entity.getDeductibleAmount())
                .currency(entity.getCurrency())
                .reminderId(entity.getReminderId())
                .reminderOffsetDays(entity.getReminderOffsetDays())
                .contactPhone(entity.getContactPhone())
                .contactEmail(entity.getContactEmail())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
