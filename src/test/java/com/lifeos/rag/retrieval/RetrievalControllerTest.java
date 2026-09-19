package com.lifeos.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.service.DependentService;
import com.lifeos.document.dto.DocumentResponse;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.service.DocumentService;
import com.lifeos.rag.retrieval.dto.RetrievalFilters;
import com.lifeos.rag.retrieval.dto.RetrievalMatchSource;
import com.lifeos.rag.retrieval.dto.RetrievalMode;
import com.lifeos.rag.retrieval.dto.RetrievalRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RetrievalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DependentService dependentService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    private static final byte[] SAMPLE_POLICY_PDF = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 150 >> stream\n" +
            "BT /F1 12 Tf 100 700 Td (SECTION 1: Auto Collision Policy. Comprehensive collision coverage requires a $500 deductible.) Tj ET\n" +
            "endstream\n" +
            "endobj\n" +
            "xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000214 00000 n \n" +
            "trailer << /Size 5 /Root 1 0 R >>\nstartxref\n410\n%%EOF\n").getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("retrieve.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("RetrieveUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("retrieve.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("RetrieveUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    private DocumentResponse uploadAutoPolicy(UUID userId) {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Auto_Collision_Policy.pdf",
                "application/pdf",
                SAMPLE_POLICY_PDF
        );

        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .title("Auto Collision Policy 2026")
                .category(DocumentCategory.INSURANCE)
                .documentType(DocumentType.POLICY)
                .build();

        return documentService.uploadDocument(file, request, userId);
    }

    @Test
    @DisplayName("Should retrieve document chunks using HYBRID mode")
    void shouldRetrieveViaHybridMode() throws Exception {
        uploadAutoPolicy(userAId);

        RetrievalRequest request = RetrievalRequest.builder()
                .query("collision coverage deductible")
                .mode(RetrievalMode.HYBRID)
                .topK(5)
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasRelevantContext").value(true))
                .andExpect(jsonPath("$.data.retrievalMode").value(RetrievalMode.HYBRID.name()))
                .andExpect(jsonPath("$.data.results", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.results[0].documentTitle").value("Auto Collision Policy 2026"))
                .andExpect(jsonPath("$.data.results[0].content", containsString("collision coverage")))
                .andExpect(jsonPath("$.data.citations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.citations[0].sourceCitation", containsString("Auto Collision Policy 2026")))
                .andExpect(jsonPath("$.data.metadata.totalCandidatesFound", greaterThan(0)));
    }

    @Test
    @DisplayName("Should retrieve document chunks using LEXICAL mode only")
    void shouldRetrieveViaLexicalMode() throws Exception {
        uploadAutoPolicy(userAId);

        RetrievalRequest request = RetrievalRequest.builder()
                .query("collision")
                .mode(RetrievalMode.LEXICAL)
                .topK(5)
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasRelevantContext").value(true))
                .andExpect(jsonPath("$.data.retrievalMode").value(RetrievalMode.LEXICAL.name()))
                .andExpect(jsonPath("$.data.results[0].matchSource").value(RetrievalMatchSource.LEXICAL_ONLY.name()));
    }

    @Test
    @DisplayName("Should retrieve document chunks using SEMANTIC mode only")
    void shouldRetrieveViaSemanticMode() throws Exception {
        uploadAutoPolicy(userAId);

        RetrievalRequest request = RetrievalRequest.builder()
                .query("collision coverage")
                .mode(RetrievalMode.SEMANTIC)
                .topK(5)
                .minRelevanceScore(-1.0)
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasRelevantContext").value(true))
                .andExpect(jsonPath("$.data.retrievalMode").value(RetrievalMode.SEMANTIC.name()))
                .andExpect(jsonPath("$.data.results[0].matchSource").value(RetrievalMatchSource.SEMANTIC_ONLY.name()));
    }

    @Test
    @DisplayName("Multi-tenancy: User B cannot retrieve User A's chunks")
    void shouldEnforceMultiTenantIsolation() throws Exception {
        uploadAutoPolicy(userAId);

        // User B searches for exact keywords present in User A's document
        RetrievalRequest request = RetrievalRequest.builder()
                .query("collision coverage requires a $500 deductible")
                .mode(RetrievalMode.HYBRID)
                .topK(5)
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasRelevantContext").value(false))
                .andExpect(jsonPath("$.data.results", hasSize(0)))
                .andExpect(jsonPath("$.data.citations", hasSize(0)));
    }

    @Test
    @DisplayName("Should apply metadata filters (category)")
    void shouldFilterByCategory() throws Exception {
        uploadAutoPolicy(userAId);

        // Matching category: INSURANCE -> returns result
        RetrievalRequest matchingRequest = RetrievalRequest.builder()
                .query("collision")
                .filters(RetrievalFilters.builder().category(DocumentCategory.INSURANCE).build())
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasRelevantContext").value(true));

        // Non-matching category: TRAVEL -> returns 0 results
        RetrievalRequest nonMatchingRequest = RetrievalRequest.builder()
                .query("collision")
                .filters(RetrievalFilters.builder().category(DocumentCategory.TRAVEL).build())
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonMatchingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasRelevantContext").value(false))
                .andExpect(jsonPath("$.data.results", hasSize(0)));
    }

    @Test
    @DisplayName("Validation: blank query returns RFC 7807 400 Bad Request")
    void shouldReturn400OnBlankQuery() throws Exception {
        RetrievalRequest request = RetrievalRequest.builder()
                .query("   ")
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation: foreign dependent ID returns 404 Not Found")
    void shouldReturn404OnForeignDependent() throws Exception {
        // User B creates a dependent
        DependentResponse depB = dependentService.createDependent(
                userBId,
                CreateDependentRequest.builder()
                        .fullName("Bob Jr.")
                        .relationship(com.lifeos.dependent.entity.RelationshipType.CHILD)
                        .build()
        );

        // User A attempts to retrieve with User B's dependent ID -> 404
        RetrievalRequest request = RetrievalRequest.builder()
                .query("policy")
                .filters(RetrievalFilters.builder().dependentId(depB.getId()).build())
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Versioning: inactive older version chunks are excluded from retrieval")
    void shouldExcludeInactiveOlderVersionChunks() throws Exception {
        DocumentResponse doc = uploadAutoPolicy(userAId);

        // Upload version 2 with updated content
        byte[] v2Bytes = ("%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
                "4 0 obj << /Length 150 >> stream\n" +
                "BT /F1 12 Tf 100 700 Td (SECTION 1: Auto Collision Policy Version Two. Deductible reduced to $250.) Tj ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000214 00000 n \n" +
                "trailer << /Size 5 /Root 1 0 R >>\nstartxref\n410\n%%EOF\n").getBytes(StandardCharsets.UTF_8);

        MockMultipartFile newFile = new MockMultipartFile(
                "file",
                "Auto_Collision_Policy_v2.pdf",
                "application/pdf",
                v2Bytes
        );

        mockMvc.perform(multipart("/api/v1/documents/{id}/versions", doc.getId())
                        .file(newFile)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Retrieve chunks: must only return version 2 chunk
        RetrievalRequest request = RetrievalRequest.builder()
                .query("deductible")
                .mode(RetrievalMode.HYBRID)
                .filters(RetrievalFilters.builder().includeHistorical(false).build())
                .build();

        mockMvc.perform(post("/api/v1/search/retrieve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].documentVersion").value(2))
                .andExpect(jsonPath("$.data.results[0].content", containsString("Version Two")));
    }
}
