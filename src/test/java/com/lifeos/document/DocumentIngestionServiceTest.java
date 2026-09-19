package com.lifeos.document;

import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.document.dto.DocumentIngestionStatusResponse;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentChunkEntity;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.entity.IngestionStatus;
import com.lifeos.document.repository.DocumentChunkRepository;
import com.lifeos.document.service.DocumentIngestionService;
import com.lifeos.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DocumentIngestionServiceTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentIngestionService documentIngestionService;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private AuthService authService;

    private UUID userId;

    private static final byte[] VALID_TEXT_PDF = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 110 >> stream\n" +
            "BT /F1 12 Tf 100 700 Td (SECTION 1: LifeOS Auto Ingestion Test Clause. Coverage applies to all dependents.) Tj ET\n" +
            "endstream\n" +
            "endobj\n" +
            "xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000214 00000 n \n" +
            "trailer << /Size 5 /Root 1 0 R >>\nstartxref\n370\n%%EOF\n").getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        RegisterRequest req = RegisterRequest.builder()
                .email("ingest.test." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Ingest")
                .lastName("Tester")
                .build();
        AuthResponse res = authService.register(req);
        userId = res.getUser().getId();
    }

    @Test
    @DisplayName("Should successfully ingest document and persist chunks with active status")
    void shouldIngestDocumentAndPersistChunks() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Ingestion_Test.pdf",
                "application/pdf",
                VALID_TEXT_PDF
        );

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .title("Ingestion Test Document")
                .category(DocumentCategory.INSURANCE)
                .documentType(DocumentType.POLICY)
                .build();

        DocumentResponse doc = documentService.uploadDocument(file, request, userId);

        assertThat(doc.getIngestionStatus()).isEqualTo(IngestionStatus.PROCESSED);
        assertThat(doc.getChunkCount()).isGreaterThanOrEqualTo(1);

        List<DocumentChunkEntity> activeChunks = documentChunkRepository
                .findAllByDocumentIdAndUserIdAndIsActiveTrueOrderByChunkIndexAsc(doc.getId(), userId);

        assertThat(activeChunks).isNotEmpty();
        DocumentChunkEntity firstChunk = activeChunks.get(0);
        assertThat(firstChunk.getChunkIndex()).isEqualTo(0);
        assertThat(firstChunk.getIsActive()).isTrue();
        assertThat(firstChunk.getPageNumber()).isEqualTo(1);
        assertThat(firstChunk.getContent()).contains("LifeOS Auto Ingestion Test Clause");

        DocumentIngestionStatusResponse status = documentIngestionService.getIngestionStatus(doc.getId(), userId);
        assertThat(status.getIngestionStatus()).isEqualTo(IngestionStatus.PROCESSED);
        assertThat(status.getChunkCount()).isGreaterThanOrEqualTo(1);
        assertThat(status.getEmbeddingModel()).isNotEmpty();
    }

    @Test
    @DisplayName("Should reprocess document idempotently without producing duplicate chunks")
    void shouldReprocessDocumentIdempotently() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Reprocess_Test.pdf",
                "application/pdf",
                VALID_TEXT_PDF
        );

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .title("Reprocess Test Document")
                .category(DocumentCategory.TAX)
                .documentType(DocumentType.TAX_RETURN)
                .build();

        DocumentResponse doc = documentService.uploadDocument(file, request, userId);
        long initialCount = documentChunkRepository.countByDocumentIdAndUserIdAndIsActiveTrue(doc.getId(), userId);

        // Reprocess synchronously
        documentIngestionService.ingestDocument(doc.getId(), userId);

        long postReprocessCount = documentChunkRepository.countByDocumentIdAndUserIdAndIsActiveTrue(doc.getId(), userId);
        assertThat(postReprocessCount).isEqualTo(initialCount);
    }
}
