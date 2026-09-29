package com.lifeos.audit.service;

import com.lifeos.audit.dto.AuditLogResponse;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditLogEntity;
import com.lifeos.audit.entity.AuditOutcome;
import com.lifeos.audit.event.SecurityAuditEvent;
import com.lifeos.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuditService implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    @Transactional
    public void handleSecurityAuditEvent(SecurityAuditEvent event) {
        logEvent(event.getUserId(), event.getEventType(), event.getOutcome(),
                event.getIpAddress(), event.getUserAgent(), event.getDetails());
    }

    @Override
    @Transactional
    public void logEvent(UUID userId, AuditEventType eventType, AuditOutcome outcome, String ipAddress, String userAgent, String details) {
        try {
            AuditLogEntity entity = AuditLogEntity.builder()
                    .userId(userId)
                    .eventType(eventType)
                    .actionOutcome(outcome != null ? outcome : AuditOutcome.SUCCESS)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(details != null && !details.isBlank() ? details : "{}")
                    .createdAt(OffsetDateTime.now())
                    .build();

            auditLogRepository.save(entity);
            log.info("Recorded security audit event [{}] for user [{}] with outcome [{}]",
                    eventType, userId != null ? userId : "ANONYMOUS", outcome);
        } catch (Exception e) {
            log.error("Failed to persist security audit event [{}]: {}", eventType, e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogs(UUID userId, AuditEventType eventType, Pageable pageable) {
        Page<AuditLogEntity> page;
        if (eventType != null) {
            page = auditLogRepository.findByUserIdAndEventTypeOrderByCreatedAtDesc(userId, eventType, pageable);
        } else {
            page = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(AuditLogResponse::fromEntity);
    }
}
