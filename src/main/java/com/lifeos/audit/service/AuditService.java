package com.lifeos.audit.service;

import com.lifeos.audit.dto.AuditLogResponse;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditService {
    void logEvent(UUID userId, AuditEventType eventType, AuditOutcome outcome, String ipAddress, String userAgent, String details);
    Page<AuditLogResponse> getLogs(UUID userId, AuditEventType eventType, Pageable pageable);
}
