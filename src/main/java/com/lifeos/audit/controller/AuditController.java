package com.lifeos.audit.controller;

import com.lifeos.audit.dto.AuditLogResponse;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.service.AuditService;
import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Security Audit", description = "Immutable security event tracking and activity logs")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    @Operation(summary = "Get paginated security audit logs for the authenticated user")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) AuditEventType eventType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<AuditLogResponse> logsPage = auditService.getLogs(currentUserId, eventType, pageable);
        return ResponseEntity.ok(ApiResponse.success(logsPage, "Audit logs retrieved successfully"));
    }
}
