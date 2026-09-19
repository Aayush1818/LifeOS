package com.lifeos.document.service;

import com.lifeos.common.exception.FileStorageException;
import com.lifeos.common.exception.InvalidDocumentException;
import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.document.dto.DocumentChunkResponse;
import com.lifeos.document.dto.DocumentDetailResponse;
import com.lifeos.document.dto.DocumentIngestionStatusResponse;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.IngestionStatus;
import com.lifeos.document.extractor.DocumentTextExtractor;
import com.lifeos.document.extractor.ExtractionResult;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.document.repository.JdbcDocumentChunkRepository;
import com.lifeos.document.storage.DocumentStorageService;
import com.lifeos.document.storage.LocalStorageService;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DependentRepository dependentRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentIngestionService documentIngestionService;
    private final JdbcDocumentChunkRepository jdbcDocumentChunkRepository;
    private final long maxFileSizeBytes;
    private final Set<String> allowedMimeTypes;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            DependentRepository dependentRepository,
            DocumentStorageService documentStorageService,
            DocumentTextExtractor documentTextExtractor,
            DocumentIngestionService documentIngestionService,
            JdbcDocumentChunkRepository jdbcDocumentChunkRepository,
            com.lifeos.document.storage.StorageProperties storageProperties
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.dependentRepository = dependentRepository;
        this.documentStorageService = documentStorageService;
        this.documentTextExtractor = documentTextExtractor;
        this.documentIngestionService = documentIngestionService;
        this.jdbcDocumentChunkRepository = jdbcDocumentChunkRepository;
        this.maxFileSizeBytes = storageProperties.getMaxFileSizeBytes();
        this.allowedMimeTypes = new HashSet<>(storageProperties.getAllowedMimeTypes());
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, UploadDocumentRequest request, UUID userId) {
        validateFile(file);

        UserEntity user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DependentEntity dependent = null;
        if (request.getDependentId() != null) {
            dependent = dependentRepository.findByIdAndUserIdAndIsDeletedFalse(request.getDependentId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dependent not found with id: " + request.getDependentId()));
        }

        String detectedMimeType = detectAndValidateMimeType(file);
        String checksumSha256 = calculateChecksum(file);

        // 1. Store original file on storage
        String storagePath;
        try (InputStream is = file.getInputStream()) {
            storagePath = documentStorageService.store(is, file.getOriginalFilename(), userId);
        } catch (Exception e) {
            log.error("Failed to write document to storage: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to write document file to storage", e);
        }

        // 2. Extract text & metadata (graceful failure handling)
        ExtractionResult extraction;
        try (InputStream is = file.getInputStream()) {
            extraction = documentTextExtractor.extract(is, file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("Extraction pipeline warning for [{}]: {}", file.getOriginalFilename(), e.getMessage());
            extraction = ExtractionResult.failure(detectedMimeType, e.getMessage(), Collections.emptyMap());
        }

        IngestionStatus status = extraction.isSuccess() ? IngestionStatus.PROCESSED : IngestionStatus.EXTRACTION_FAILED;

        // 3. Database persistence with compensation rollback on error
        try {
            DocumentEntity document = DocumentEntity.builder()
                    .user(user)
                    .dependent(dependent)
                    .title(request.getTitle())
                    .originalFilename(sanitizeFilename(file.getOriginalFilename()))
                    .storagePath(storagePath)
                    .mimeType(detectedMimeType)
                    .fileSize(file.getSize())
                    .category(request.getCategory())
                    .documentType(request.getDocumentType())
                    .issueDate(request.getIssueDate())
                    .expiryDate(request.getExpiryDate())
                    .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                    .checksumSha256(checksumSha256)
                    .ingestionStatus(status)
                    .version(1)
                    .extractedText(extraction.getExtractedText())
                    .metadata(extraction.getMetadata())
                    .extractionError(extraction.getErrorMessage())
                    .build();

            DocumentEntity saved = documentRepository.save(document);
            if (status == IngestionStatus.PROCESSED) {
                try {
                    documentIngestionService.ingestDocument(saved.getId(), userId);
                    saved = documentRepository.findById(saved.getId()).orElse(saved);
                } catch (Exception e) {
                    log.warn("Ingestion pipeline error for document [{}]: {}", saved.getId(), e.getMessage());
                }
            }
            log.info("User [{}] uploaded document [{}] version 1 (status: {})", userId, saved.getId(), saved.getIngestionStatus());
            return DocumentResponse.fromEntity(saved);
        } catch (Exception e) {
            log.error("Database persistence failed for document; executing storage compensation cleanup for [{}]", storagePath);
            if (documentStorageService instanceof LocalStorageService localStorageService) {
                localStorageService.deleteQuietly(storagePath, userId);
            } else {
                try {
                    documentStorageService.delete(storagePath, userId);
                } catch (Exception ignored) {}
            }
            throw e;
        }
    }

    @Transactional
    public DocumentResponse uploadNewVersion(UUID documentId, MultipartFile file, UUID userId) {
        validateFile(file);

        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        String detectedMimeType = detectAndValidateMimeType(file);
        String checksumSha256 = calculateChecksum(file);

        String newStoragePath;
        try (InputStream is = file.getInputStream()) {
            newStoragePath = documentStorageService.store(is, file.getOriginalFilename(), userId);
        } catch (Exception e) {
            throw new FileStorageException("Failed to write new version to storage", e);
        }

        ExtractionResult extraction;
        try (InputStream is = file.getInputStream()) {
            extraction = documentTextExtractor.extract(is, file.getOriginalFilename());
        } catch (Exception e) {
            extraction = ExtractionResult.failure(detectedMimeType, e.getMessage(), Collections.emptyMap());
        }

        IngestionStatus status = extraction.isSuccess() ? IngestionStatus.PROCESSED : IngestionStatus.EXTRACTION_FAILED;

        document.setVersion(document.getVersion() + 1);
        document.setStoragePath(newStoragePath);
        document.setOriginalFilename(sanitizeFilename(file.getOriginalFilename()));
        document.setMimeType(detectedMimeType);
        document.setFileSize(file.getSize());
        document.setChecksumSha256(checksumSha256);
        document.setIngestionStatus(status);
        document.setExtractedText(extraction.getExtractedText());
        document.setMetadata(extraction.getMetadata());
        document.setExtractionError(extraction.getErrorMessage());

        DocumentEntity updated = documentRepository.save(document);
        if (status == IngestionStatus.PROCESSED) {
            try {
                documentIngestionService.ingestDocument(updated.getId(), userId);
                updated = documentRepository.findById(updated.getId()).orElse(updated);
            } catch (Exception e) {
                log.warn("Ingestion pipeline error for new version of document [{}]: {}", updated.getId(), e.getMessage());
            }
        }
        log.info("User [{}] uploaded new version [{}] for document [{}]", userId, updated.getVersion(), documentId);
        return DocumentResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse getDocument(UUID id, UUID userId) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));
        return DocumentDetailResponse.fromEntity(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(UUID userId, DocumentCategory category, UUID dependentId, Pageable pageable) {
        Page<DocumentEntity> page;
        if (category != null) {
            page = documentRepository.findAllByUserIdAndCategoryAndIsDeletedFalse(userId, category, pageable);
        } else if (dependentId != null) {
            page = documentRepository.findAllByUserIdAndDependentIdAndIsDeletedFalse(userId, dependentId, pageable);
        } else {
            page = documentRepository.findAllByUserIdAndIsDeletedFalse(userId, pageable);
        }
        return page.map(DocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DocumentDownload loadDocumentForDownload(UUID id, UUID userId) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        Resource resource = documentStorageService.loadAsResource(document.getStoragePath(), userId);
        log.info("User [{}] requested download for document [{}]", userId, id);
        return new DocumentDownload(resource, document.getOriginalFilename(), document.getMimeType(), document.getFileSize());
    }

    @Transactional
    public void deleteDocument(UUID id, UUID userId) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        // Two-stage deletion:
        // 1. Immediately soft-delete in database
        document.setDeleted(true);
        document.setIngestionStatus(IngestionStatus.DELETED);
        documentRepository.save(document);
        log.info("User [{}] soft-deleted document [{}] in database", userId, id);

        // 2. Purge chunks from vector store (fault-tolerant)
        try {
            jdbcDocumentChunkRepository.deleteAllChunksByDocument(id);
        } catch (Exception e) {
            log.warn("Chunk deletion warning for document [{}]: {}", id, e.getMessage());
        }

        // 3. Purge physical storage (fault-tolerant)
        try {
            documentStorageService.delete(document.getStoragePath(), userId);
        } catch (Exception e) {
            log.warn("Storage deletion warning for document [{}]: {}", id, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public DocumentIngestionStatusResponse getIngestionStatus(UUID id, UUID userId) {
        return documentIngestionService.getIngestionStatus(id, userId);
    }

    @Transactional(readOnly = true)
    public Page<DocumentChunkResponse> listChunks(UUID id, UUID userId, Pageable pageable) {
        return documentIngestionService.listChunks(id, userId, pageable);
    }

    @Transactional
    public DocumentResponse reprocessDocument(UUID id, UUID userId, boolean force) {
        return documentIngestionService.reprocessDocument(id, userId, force);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("File cannot be null or empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            long maxMb = maxFileSizeBytes / (1024 * 1024);
            throw new InvalidDocumentException("File size exceeds maximum allowed limit of " + maxMb + " MB");
        }
    }

    private String detectAndValidateMimeType(MultipartFile file) {
        String detectedMimeType;
        try (InputStream is = new BufferedInputStream(file.getInputStream())) {
            detectedMimeType = documentTextExtractor.detectMimeType(is, file.getOriginalFilename());
        } catch (Exception e) {
            throw new InvalidDocumentException("Failed to inspect file magic bytes: " + e.getMessage());
        }

        if (!allowedMimeTypes.contains(detectedMimeType)) {
            throw new InvalidDocumentException("Unsupported or invalid document format: " + detectedMimeType +
                    ". Allowed types: " + allowedMimeTypes);
        }

        return detectedMimeType;
    }

    private String calculateChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new FileStorageException("Failed to calculate document SHA-256 checksum", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document.bin";
        }
        return java.nio.file.Paths.get(filename).getFileName().toString();
    }

    public record DocumentDownload(Resource resource, String filename, String mimeType, Long contentLength) {}
}
