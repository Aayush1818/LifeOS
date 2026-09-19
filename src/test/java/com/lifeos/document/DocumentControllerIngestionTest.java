package com.lifeos.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.entity.IngestionStatus;
import com.lifeos.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIngestionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private DocumentService documentService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    private static final byte[] SAMPLE_PDF_BYTES = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 120 >> stream\n" +
            "BT /F1 12 Tf 100 700 Td (SECTION 1: LifeOS RAG Ingestion Test. This document verifies chunking and embeddings.) Tj ET\n" +
            "endstream\n" +
            "endobj\n" +
            "xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000214 00000 n \n" +
            "trailer << /Size 5 /Root 1 0 R >>\nstartxref\n380\n%%EOF\n").getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("rag.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("RagUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("rag.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("RagUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    private DocumentResponse uploadTestDocument(UUID userId) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Health_Policy_RAG.pdf",
                "application/pdf",
                SAMPLE_PDF_BYTES
        );

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .title("Health Policy RAG")
                .category(DocumentCategory.INSURANCE)
                .documentType(DocumentType.POLICY)
                .build();

        return documentService.uploadDocument(file, request, userId);
    }

    @Test
    @DisplayName("Should retrieve ingestion status for an uploaded document")
    void shouldRetrieveIngestionStatus() throws Exception {
        DocumentResponse doc = uploadTestDocument(userAId);

        mockMvc.perform(get("/api/v1/documents/{id}/ingestion-status", doc.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId").value(doc.getId().toString()))
                .andExpect(jsonPath("$.data.ingestionStatus").value(IngestionStatus.PROCESSED.name()))
                .andExpect(jsonPath("$.data.chunkCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.embeddingModel").isNotEmpty());
    }

    @Test
    @DisplayName("Should retrieve paginated chunks for an uploaded document")
    void shouldRetrievePaginatedChunks() throws Exception {
        DocumentResponse doc = uploadTestDocument(userAId);

        mockMvc.perform(get("/api/v1/documents/{id}/chunks", doc.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data.content[0].pageNumber").value(1))
                .andExpect(jsonPath("$.data.content[0].content", containsString("LifeOS RAG Ingestion Test")))
                .andExpect(jsonPath("$.data.content[0].isActive").value(true));
    }

    @Test
    @DisplayName("Should trigger reprocess endpoint successfully")
    void shouldTriggerReprocess() throws Exception {
        DocumentResponse doc = uploadTestDocument(userAId);

        mockMvc.perform(post("/api/v1/documents/{id}/reprocess", doc.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"force\": true}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(doc.getId().toString()));
    }

    @Test
    @DisplayName("Multi-tenancy: User B cannot access User A's ingestion status or chunks")
    void shouldEnforceTenantIsolation() throws Exception {
        DocumentResponse docA = uploadTestDocument(userAId);

        // User B attempts to access User A's ingestion status -> 404
        mockMvc.perform(get("/api/v1/documents/{id}/ingestion-status", docA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B attempts to access User A's chunks -> 404
        mockMvc.perform(get("/api/v1/documents/{id}/chunks", docA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B attempts to reprocess User A's document -> 404
        mockMvc.perform(post("/api/v1/documents/{id}/reprocess", docA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"force\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Versioning: uploading a new version should increment version and maintain active chunks")
    void shouldDeactivateOldChunksOnNewVersion() throws Exception {
        DocumentResponse doc = uploadTestDocument(userAId);

        // Upload version 2
        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "Health_Policy_RAG_v2.pdf",
                "application/pdf",
                SAMPLE_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/v1/documents/{id}/versions", doc.getId())
                        .file(newFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.chunkCount", greaterThanOrEqualTo(1)));

        // Check ingestion status
        mockMvc.perform(get("/api/v1/documents/{id}/ingestion-status", doc.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));
    }
}
