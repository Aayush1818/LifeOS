package com.lifeos.rag.retrieval.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API controller for Hybrid RAG Retrieval.
 * Provides authenticated multi-tenant access to PostgreSQL FTS and pgvector semantic retrieval.
 */
@RestController
@RequestMapping("/api/v1/search/retrieve")
@RequiredArgsConstructor
@Tag(name = "Hybrid RAG Retrieval", description = "Endpoints for hybrid lexical and semantic document chunk retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    @PostMapping
    @Operation(summary = "Retrieve most relevant document chunks via hybrid lexical FTS and pgvector semantic search")
    public ResponseEntity<ApiResponse<RetrievalResponse>> retrieve(
            @Valid @RequestBody RetrievalRequest request) {

        UUID userId = SecurityUtils.getCurrentUserId();
        RetrievalResponse response = retrievalService.retrieve(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Retrieval completed successfully"));
    }
}
