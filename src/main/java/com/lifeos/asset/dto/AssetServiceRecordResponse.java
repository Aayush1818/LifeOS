package com.lifeos.asset.dto;

import com.lifeos.asset.entity.AssetServiceRecordEntity;
import com.lifeos.asset.entity.ServiceStatus;
import com.lifeos.asset.entity.ServiceType;
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
public class AssetServiceRecordResponse {

    private UUID id;
    private UUID userId;
    private UUID assetId;
    private String assetName;
    private LocalDate serviceDate;
    private ServiceType serviceType;
    private String serviceProvider;
    private String description;
    private BigDecimal cost;
    private String currency;
    private ServiceStatus status;
    private UUID invoiceId;
    private UUID warrantyClaimId;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AssetServiceRecordResponse fromEntity(AssetServiceRecordEntity entity) {
        if (entity == null) return null;
        return AssetServiceRecordResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .assetName(entity.getAsset() != null ? entity.getAsset().getName() : null)
                .serviceDate(entity.getServiceDate())
                .serviceType(entity.getServiceType())
                .serviceProvider(entity.getServiceProvider())
                .description(entity.getDescription())
                .cost(entity.getCost())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .invoiceId(entity.getInvoice() != null ? entity.getInvoice().getId() : null)
                .warrantyClaimId(entity.getWarrantyClaim() != null ? entity.getWarrantyClaim().getId() : null)
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
