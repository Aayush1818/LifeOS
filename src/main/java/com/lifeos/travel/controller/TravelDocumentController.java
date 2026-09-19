package com.lifeos.travel.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.travel.dto.TravelDocumentLinkResponse;
import com.lifeos.travel.service.TravelDocumentService;
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
@RequestMapping("/api/v1/travel/documents")
@RequiredArgsConstructor
@Tag(name = "Travel Documents", description = "Travel documents, tickets, visas, boarding passes, and hotel confirmations")
public class TravelDocumentController {

    private final TravelDocumentService travelDocumentService;

    @GetMapping
    @Operation(summary = "List travel documents", description = "Retrieves paginated travel documents for the authenticated user.")
    public ResponseEntity<ApiResponse<Page<TravelDocumentLinkResponse>>> listTravelDocuments(
            @RequestParam(required = false) DocumentType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Page<TravelDocumentLinkResponse> page = travelDocumentService.listTravelDocuments(userId, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Travel documents retrieved successfully"));
    }
}
