package com.lifeos.asset.dto;

import com.lifeos.asset.entity.InvoiceItemEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDto {

    private UUID id;
    private UUID assetId;
    private String assetName;

    @NotBlank(message = "Item description is required")
    private String itemDescription;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be positive or zero")
    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
    private String notes;

    public static InvoiceItemDto fromEntity(InvoiceItemEntity entity) {
        if (entity == null) return null;
        return InvoiceItemDto.builder()
                .id(entity.getId())
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .assetName(entity.getAsset() != null ? entity.getAsset().getName() : null)
                .itemDescription(entity.getItemDescription())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalPrice(entity.getTotalPrice())
                .notes(entity.getNotes())
                .build();
    }
}
