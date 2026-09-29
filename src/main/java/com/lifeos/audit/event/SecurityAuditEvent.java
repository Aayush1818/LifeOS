package com.lifeos.audit.event;

import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditOutcome;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SecurityAuditEvent {
    private final UUID userId;
    private final AuditEventType eventType;
    private final AuditOutcome outcome;
    private final String ipAddress;
    private final String userAgent;
    private final String details;
}
