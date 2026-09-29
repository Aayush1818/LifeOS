package com.lifeos.insight.dto;

import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResponse {
    private UUID id;
    private InsightType insightType;
    private InsightSeverity severity;
    private String title;
    private String description;
    private InsightActionType actionType;
    private Map<String, Object> actionPayload;
    private boolean isDismissed;
    private boolean isActioned;
    private OffsetDateTime createdAt;

    public static InsightResponse fromEntity(InsightEntity entity) {
        return InsightResponse.builder()
                .id(entity.getId())
                .insightType(entity.getInsightType())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .actionType(entity.getActionType())
                .actionPayload(entity.getActionPayload())
                .isDismissed(entity.isDismissed())
                .isActioned(entity.isActioned())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
