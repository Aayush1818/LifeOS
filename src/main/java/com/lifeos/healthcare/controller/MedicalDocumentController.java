package com.lifeos.healthcare.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.healthcare.dto.MedicalDocumentLinkResponse;
import com.lifeos.healthcare.service.MedicalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/healthcare/documents")
@RequiredArgsConstructor
@Tag(name = "Medical Documents", description = "Medical records, prescriptions, lab reports, and consultation summaries (Non-Diagnostic)")
public class MedicalDocumentController {

    private final MedicalDocumentService medicalDocumentService;

    @GetMapping
    @Operation(summary = "List medical documents", description = "Retrieves paginated medical documents for the authenticated user with optional dependent filter.")
    public ResponseEntity<ApiResponse<Page<MedicalDocumentLinkResponse>>> listMedicalDocuments(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) UUID dependentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserId();
        Page<MedicalDocumentLinkResponse> page = medicalDocumentService.listMedicalDocuments(userId, type, dependentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Medical documents retrieved successfully"));
    }
}
