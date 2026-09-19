package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ServiceStatus;
import com.lifeos.asset.entity.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateAssetServiceRecordRequest {

    @NotNull(message = "Service date is required")
    private LocalDate serviceDate;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Service provider is required")
    private String serviceProvider;

    @NotBlank(message = "Description is required")
    private String description;

    @PositiveOrZero(message = "Cost must be positive or zero")
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO.setScale(2);

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private ServiceStatus status = ServiceStatus.COMPLETED;

    private UUID invoiceId;
    private UUID warrantyClaimId;
    private String notes;
}
