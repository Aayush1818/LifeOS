package com.lifeos.audit.repository;

import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLogEntity> findByUserIdAndEventTypeOrderByCreatedAtDesc(UUID userId, AuditEventType eventType, Pageable pageable);
}
