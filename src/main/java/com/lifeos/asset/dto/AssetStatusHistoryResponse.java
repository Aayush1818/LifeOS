package com.lifeos.asset.dto;

import com.lifeos.asset.entity.AssetStatusHistoryEntity;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetStatusHistoryResponse {

    private UUID id;
    private UUID assetId;
    private UUID userId;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private OffsetDateTime changedAt;

    public static AssetStatusHistoryResponse fromEntity(AssetStatusHistoryEntity entity) {
        if (entity == null) return null;
        return AssetStatusHistoryResponse.builder()
                .id(entity.getId())
                .assetId(entity.getAsset() != null ? entity.getAsset().getId() : null)
                .userId(entity.getUserId())
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .reason(entity.getReason())
                .changedAt(entity.getChangedAt())
                .build();
    }
}
