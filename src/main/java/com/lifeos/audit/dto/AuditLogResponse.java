package com.lifeos.audit.dto;

import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditLogEntity;
import com.lifeos.audit.entity.AuditOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private AuditEventType eventType;
    private AuditOutcome actionOutcome;
    private String ipAddress;
    private String userAgent;
    private String details;
    private OffsetDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .eventType(entity.getEventType())
                .actionOutcome(entity.getActionOutcome())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
