package com.lifeos.asset.dto;

import com.lifeos.asset.entity.AssetCategory;
import com.lifeos.asset.entity.AssetEntity;
import com.lifeos.asset.entity.AssetStatus;
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
public class AssetResponse {

    private UUID id;
    private UUID userId;
    private UUID dependentId;
    private String dependentName;
    private String name;
    private AssetCategory category;
    private String brand;
    private String modelNumber;
    private String serialNumber;
    private LocalDate purchaseDate;
    private LocalDate returnDeadline;
    private BigDecimal purchasePrice;
    private String currency;
    private UUID primaryInvoiceId;
    private String location;
    private AssetStatus status;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static AssetResponse fromEntity(AssetEntity entity) {
        if (entity == null) return null;
        return AssetResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .dependentId(entity.getDependent() != null ? entity.getDependent().getId() : null)
                .dependentName(entity.getDependent() != null ? entity.getDependent().getFullName() : null)
                .name(entity.getName())
                .category(entity.getCategory())
                .brand(entity.getBrand())
                .modelNumber(entity.getModelNumber())
                .serialNumber(entity.getSerialNumber())
                .purchaseDate(entity.getPurchaseDate())
                .returnDeadline(entity.getReturnDeadline())
                .purchasePrice(entity.getPurchasePrice())
                .currency(entity.getCurrency())
                .primaryInvoiceId(entity.getPrimaryInvoice() != null ? entity.getPrimaryInvoice().getId() : null)
                .location(entity.getLocation())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
