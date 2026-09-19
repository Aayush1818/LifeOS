package com.lifeos.asset.dto;

import com.lifeos.asset.entity.AssetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAssetStatusRequest {

    @NotNull(message = "New asset status is required")
    private AssetStatus status;

    private String reason;
}
