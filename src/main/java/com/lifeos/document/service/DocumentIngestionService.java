package com.lifeos.document.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.document.chunking.DocumentChunk;
import com.lifeos.document.chunking.DocumentChunker;
import com.lifeos.document.dto.DocumentChunkResponse;
import com.lifeos.document.dto.DocumentIngestionStatusResponse;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.embedding.EmbeddingProvider;
import com.lifeos.document.entity.DocumentChunkEntity;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.entity.IngestionStatus;
import com.lifeos.document.extractor.DocumentTextExtractor;
import com.lifeos.document.extractor.ExtractionResult;
import com.lifeos.document.extractor.PageContent;
import com.lifeos.document.repository.ChunkInsertDto;
import com.lifeos.document.repository.DocumentChunkRepository;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.document.repository.JdbcDocumentChunkRepository;
import com.lifeos.document.storage.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator for Phase 11 Document Intelligence & RAG Ingestion Pipeline.
 * Handles validation, extraction, chunking, vector embedding generation, and idempotent persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final JdbcDocumentChunkRepository jdbcDocumentChunkRepository;
    private final DocumentStorageService documentStorageService;
    private final DocumentTextExtractor documentTextExtractor;
    private final DocumentChunker documentChunker;
    private final EmbeddingProvider embeddingProvider;

    /**
     * Executes the complete ingestion pipeline synchronously.
     */
    @Transactional
    public void ingestDocument(UUID documentId, UUID userId) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        log.info("Starting ingestion for document [{}] v{} (user: {})", documentId, document.getVersion(), userId);
        document.setIngestionStatus(IngestionStatus.PROCESSING);
        document.setExtractionError(null);
        document = documentRepository.save(document);

        // 1. Text & Layout Extraction
        ExtractionResult extraction;
        try {
            Resource resource = documentStorageService.loadAsResource(document.getStoragePath(), userId);
            try (InputStream is = resource.getInputStream()) {
                extraction = documentTextExtractor.extract(is, document.getOriginalFilename());
            }
        } catch (Exception e) {
            log.error("Storage read failure during ingestion for document [{}]: {}", documentId, e.getMessage(), e);
            document.setIngestionStatus(IngestionStatus.EXTRACTION_FAILED);
            document.setExtractionError("Failed to read document from storage: " + e.getMessage());
            documentRepository.save(document);
            return;
        }

        if (!extraction.isSuccess()) {
            log.warn("Text extraction failed for document [{}]: {}", documentId, extraction.getErrorMessage());
            document.setIngestionStatus(IngestionStatus.EXTRACTION_FAILED);
            document.setExtractionError(extraction.getErrorMessage());
            documentRepository.save(document);
            return;
        }

        String extractedText = extraction.getExtractedText();
        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("Document [{}] contains no extractable text", documentId);
            document.setIngestionStatus(IngestionStatus.EXTRACTION_FAILED);
            document.setExtractionError("Document contains no extractable text; OCR or valid content required");
            document.setExtractedText("");
            document.setChunkCount(0);
            documentRepository.save(document);
            return;
        }

        // 2. Layout-Aware Sliding Window Chunking
        List<PageContent> pages = extraction.getPages();
        List<DocumentChunk> chunks = documentChunker.chunk(
                document.getId(),
                document.getVersion(),
                (document.getTitle() != null && !document.getTitle().isBlank()) ? document.getTitle() : document.getOriginalFilename(),
                pages
        );

        if (chunks.isEmpty()) {
            // Fallback for non-empty text that didn't pass strict sentence chunker bounds
            UUID fallbackChunkId = UUID.nameUUIDFromBytes(
                    (document.getId() + ":" + document.getVersion() + ":0").getBytes(StandardCharsets.UTF_8)
            );
            chunks = List.of(DocumentChunk.builder()
                    .id(fallbackChunkId)
                    .chunkIndex(0)
                    .pageNumber(1)
                    .sectionTitle("General")
                    .content(extractedText)
                    .tokenCount(Math.max(1, extractedText.length() / DocumentChunker.CHARS_PER_TOKEN))
                    .charCount(extractedText.length())
                    .build());
        }

        // 3. Dense Vector Embedding Generation
        List<String> chunkContents = chunks.stream().map(DocumentChunk::getContent).toList();
        List<float[]> embeddings;
        try {
            embeddings = embeddingProvider.generateEmbeddings(chunkContents);
        } catch (Exception e) {
            log.error("Embedding provider failure for document [{}]: {}", documentId, e.getMessage(), e);
            document.setIngestionStatus(IngestionStatus.EMBEDDING_FAILED);
            document.setExtractionError("Embedding generation failed: " + e.getMessage());
            Map<String, Object> meta = document.getMetadata() != null ? document.getMetadata() : new HashMap<>();
            meta.put("embedding_error", e.getMessage());
            meta.put("failed_provider", embeddingProvider.getProviderName());
            document.setMetadata(meta);
            documentRepository.save(document);
            return;
        }

        // 4. Batch Persistence with Idempotency & Version Deactivation
        jdbcDocumentChunkRepository.deleteChunksByDocumentAndVersion(document.getId(), document.getVersion());

        List<ChunkInsertDto> chunkDtos = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] vector = (i < embeddings.size()) ? embeddings.get(i) : null;

            Map<String, Object> chunkMeta = new HashMap<>(chunk.getMetadata());
            chunkMeta.put("original_filename", document.getOriginalFilename());
            chunkMeta.put("mime_type", document.getMimeType());
            chunkMeta.put("category", document.getCategory().name());
            if (document.getDependent() != null) {
                chunkMeta.put("dependent_id", document.getDependent().getId().toString());
            }

            chunkDtos.add(ChunkInsertDto.builder()
                    .id(chunk.getId())
                    .documentId(document.getId())
                    .userId(userId)
                    .documentVersion(document.getVersion())
                    .chunkIndex(chunk.getChunkIndex())
                    .content(chunk.getContent())
                    .embedding(vector)
                    .pageNumber(chunk.getPageNumber())
                    .sectionTitle(chunk.getSectionTitle())
                    .tokenCount(chunk.getTokenCount())
                    .charCount(chunk.getCharCount())
                    .isActive(true)
                    .embeddingModel(embeddingProvider.getModelName())
                    .metadata(chunkMeta)
                    .build());
        }

        jdbcDocumentChunkRepository.batchInsertChunks(chunkDtos);

        // Invalidate older versions if this is a newly uploaded version (v > 1)
        if (document.getVersion() > 1) {
            int deactivated = jdbcDocumentChunkRepository.deactivateOldVersions(document.getId(), document.getVersion());
            log.info("Deactivated {} older version chunks for document [{}]", deactivated, documentId);
        }

        // 5. Finalize Document Record
        document.setExtractedText(extractedText);
        document.setChunkCount(chunks.size());
        document.setEmbeddingModel(embeddingProvider.getModelName());
        document.setIngestedAt(OffsetDateTime.now());
        document.setIngestionStatus(IngestionStatus.PROCESSED);
        document.setExtractionError(null);

        Map<String, Object> docMeta = document.getMetadata() != null ? document.getMetadata() : new HashMap<>();
        docMeta.put("Page-Count", extraction.getPageCount());
        docMeta.put("Chunk-Count", chunks.size());
        docMeta.put("Embedding-Model", embeddingProvider.getModelName());
        docMeta.put("Embedding-Dimensions", embeddingProvider.getDimension());
        document.setMetadata(docMeta);

        documentRepository.save(document);
        log.info("Document [{}] v{} successfully ingested with {} chunks (model: {})",
                documentId, document.getVersion(), chunks.size(), embeddingProvider.getModelName());
    }

    /**
     * Triggers asynchronous background ingestion using the bounded thread pool.
     */
    @Async("documentIngestionExecutor")
    public CompletableFuture<Void> ingestDocumentAsync(UUID documentId, UUID userId) {
        try {
            ingestDocument(documentId, userId);
        } catch (Exception e) {
            log.error("Asynchronous document ingestion error for [{}]: {}", documentId, e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Retrieves detailed ingestion status and metrics for a document.
     */
    @Transactional(readOnly = true)
    public DocumentIngestionStatusResponse getIngestionStatus(UUID documentId, UUID userId) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));
        return DocumentIngestionStatusResponse.fromEntity(document);
    }

    /**
     * Lists paginated chunks belonging to a document with ownership scoping.
     */
    @Transactional(readOnly = true)
    public Page<DocumentChunkResponse> listChunks(UUID documentId, UUID userId, Pageable pageable) {
        // Enforce document ownership
        documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        Page<DocumentChunkEntity> chunkPage = documentChunkRepository
                .findAllByDocumentIdAndUserIdOrderByChunkIndexAsc(documentId, userId, pageable);

        return chunkPage.map(DocumentChunkResponse::fromEntity);
    }

    /**
     * Manually triggers reprocessing of an existing document.
     */
    @Transactional
    public DocumentResponse reprocessDocument(UUID documentId, UUID userId, boolean force) {
        DocumentEntity document = documentRepository.findByIdAndUserIdAndIsDeletedFalse(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

        if (!force && document.getIngestionStatus() == IngestionStatus.PROCESSING) {
            log.info("Document [{}] is already processing; ignoring reprocess request without force flag", documentId);
            return DocumentResponse.fromEntity(document);
        }

        document.setIngestionStatus(IngestionStatus.PROCESSING);
        document.setExtractionError(null);
        DocumentEntity saved = documentRepository.save(document);

        // Execute ingestion
        ingestDocumentAsync(documentId, userId);

        return DocumentResponse.fromEntity(saved);
    }
}
