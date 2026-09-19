package com.lifeos.asset.dto;

import com.lifeos.asset.entity.WarrantyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWarrantyStatusRequest {

    @NotNull(message = "Warranty status is required")
    private WarrantyStatus status;
}
