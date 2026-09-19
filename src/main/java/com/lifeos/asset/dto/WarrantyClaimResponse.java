package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ClaimStatus;
import com.lifeos.asset.entity.ClaimType;
import com.lifeos.asset.entity.WarrantyClaimEntity;
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
public class WarrantyClaimResponse {

    private UUID id;
    private UUID userId;
    private UUID warrantyId;
    private UUID assetId;
    private String assetName;
    private String claimNumber;
    private LocalDate claimDate;
    private ClaimType claimType;
    private ClaimStatus status;
    private String description;
    private String resolution;
    private LocalDate resolvedDate;
    private BigDecimal claimCostCovered;
    private BigDecimal outOfPocketCost;
    private String currency;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static WarrantyClaimResponse fromEntity(WarrantyClaimEntity entity) {
        if (entity == null) return null;
        return WarrantyClaimResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .warrantyId(entity.getWarranty() != null ? entity.getWarranty().getId() : null)
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .assetName(entity.getAsset() != null ? entity.getAsset().getName() : null)
                .claimNumber(entity.getClaimNumber())
                .claimDate(entity.getClaimDate())
                .claimType(entity.getClaimType())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .resolution(entity.getResolution())
                .resolvedDate(entity.getResolvedDate())
                .claimCostCovered(entity.getClaimCostCovered())
                .outOfPocketCost(entity.getOutOfPocketCost())
                .currency(entity.getCurrency())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
