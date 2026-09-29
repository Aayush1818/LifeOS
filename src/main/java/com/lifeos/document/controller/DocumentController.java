package com.lifeos.document.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.document.dto.DocumentChunkResponse;
import com.lifeos.document.dto.DocumentDetailResponse;
import com.lifeos.document.dto.DocumentIngestionStatusResponse;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.dto.ReprocessDocumentRequest;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Documents", description = "Document management, upload, download, metadata, and versioning APIs")
public class DocumentController {

    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and process document", description = "Uploads a document with metadata, performs content magic-byte MIME detection, extracts text, and stores safely.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Document successfully uploaded and processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file or unsupported MIME type"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Parameter(description = "Document binary file (PDF, DOCX, TXT, images)")
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String titleParam,
            @RequestParam(value = "category", required = false) String categoryParam,
            @RequestParam(value = "documentType", required = false) String documentTypeParam,
            @RequestParam(value = "dependentId", required = false) UUID dependentId,
            @RequestParam(value = "metadata", required = false) String metadataParam,
            @RequestPart(value = "metadata", required = false) MultipartFile metadataFile
    ) {
        UploadDocumentRequest request = null;
        if (metadataParam != null && !metadataParam.isBlank()) {
            try {
                request = objectMapper.readValue(metadataParam, UploadDocumentRequest.class);
            } catch (Exception ignored) {}
        }
        if (request == null && metadataFile != null && !metadataFile.isEmpty()) {
            try {
                request = objectMapper.readValue(metadataFile.getInputStream(), UploadDocumentRequest.class);
            } catch (Exception ignored) {}
        }
        if (request == null) {
            String fallbackTitle = (titleParam != null && !titleParam.isBlank())
                    ? titleParam.trim()
                    : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Untitled Document");
            DocumentCategory fallbackCategory = DocumentCategory.fromString(categoryParam);
            request = UploadDocumentRequest.builder()
                    .title(fallbackTitle)
                    .category(fallbackCategory)
                    .dependentId(dependentId)
                    .build();
        } else {
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                request.setTitle(titleParam != null && !titleParam.isBlank() ? titleParam.trim() : file.getOriginalFilename());
            }
            if (request.getCategory() == null) {
                request.setCategory(DocumentCategory.fromString(categoryParam));
            }
        }
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DocumentResponse response = documentService.uploadDocument(file, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Document uploaded successfully"));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload new document version", description = "Uploads a replacement binary version of an existing logical document, incrementing version number.")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadNewVersion(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DocumentResponse response = documentService.uploadNewVersion(id, file, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "New document version uploaded successfully"));
    }

    @GetMapping
    @Operation(summary = "List documents", description = "Retrieves a paginated list of documents belonging to the authenticated user, with optional category/dependent filters.")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> listDocuments(
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(required = false) UUID dependentId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<DocumentResponse> response = documentService.listDocuments(currentUserId, category, dependentId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document details", description = "Retrieves full metadata and extracted text for a document. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DocumentDetailResponse response = documentService.getDocument(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document binary", description = "Streams the stored document binary with Content-Disposition header. Returns 404 if not found or unauthorized.")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DocumentService.DocumentDownload download = documentService.loadDocumentForDownload(id, currentUserId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .body(download.resource());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Soft-deletes document and purges physical file. Returns 404 if not found or unauthorized.")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        documentService.deleteDocument(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    @GetMapping("/{id}/ingestion-status")
    @Operation(summary = "Get document ingestion and vector status", description = "Retrieves RAG ingestion lifecycle state, chunk counts, and embedding metadata.")
    public ResponseEntity<ApiResponse<DocumentIngestionStatusResponse>> getIngestionStatus(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        DocumentIngestionStatusResponse response = documentService.getIngestionStatus(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "Manually trigger document re-ingestion", description = "Re-runs text extraction, chunking, and embedding generation.")
    public ResponseEntity<ApiResponse<DocumentResponse>> reprocessDocument(
            @PathVariable UUID id,
            @RequestBody(required = false) ReprocessDocumentRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean force = request != null && request.isForce();
        DocumentResponse response = documentService.reprocessDocument(id, currentUserId, force);
        return ResponseEntity.accepted().body(ApiResponse.success(response, "Document re-ingestion initiated"));
    }

    @GetMapping("/{id}/chunks")
    @Operation(summary = "List document chunks", description = "Retrieves paginated layout-aware chunks with page numbers and section breadcrumbs for citations.")
    public ResponseEntity<ApiResponse<Page<DocumentChunkResponse>>> listChunks(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "chunkIndex") Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Page<DocumentChunkResponse> response = documentService.listChunks(id, currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
