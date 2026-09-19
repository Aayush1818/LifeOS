package com.lifeos.asset.dto;

import com.lifeos.asset.entity.AssetCategory;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetRequest {

    private String name;
    private AssetCategory category;
    private String brand;
    private String modelNumber;
    private String serialNumber;
    private LocalDate purchaseDate;
    private LocalDate returnDeadline;

    @PositiveOrZero(message = "Purchase price must be positive or zero")
    private BigDecimal purchasePrice;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private UUID dependentId;
    private UUID primaryInvoiceId;
    private String location;
    private String notes;
}
