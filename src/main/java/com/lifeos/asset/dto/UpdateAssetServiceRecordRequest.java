package com.lifeos.asset.dto;

import com.lifeos.asset.entity.ServiceStatus;
import com.lifeos.asset.entity.ServiceType;
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
public class UpdateAssetServiceRecordRequest {

    private LocalDate serviceDate;
    private ServiceType serviceType;
    private String serviceProvider;
    private String description;

    @PositiveOrZero(message = "Cost must be positive or zero")
    private BigDecimal cost;

    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private ServiceStatus status;
    private UUID invoiceId;
    private UUID warrantyClaimId;
    private String notes;
}
