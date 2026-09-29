package com.lifeos.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.repository.AuditLogRepository;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentEntity;
import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.insurance.comparison.dto.PolicyComparisonRequest;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyComparisonTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    private DocumentEntity docA1;
    private DocumentEntity docA2;
    private DocumentEntity docB1;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("comp.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Compare")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();
        UserEntity userA = userRepository.findById(userAId).orElseThrow();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("comp.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Compare")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
        UserEntity userB = userRepository.findById(userBId).orElseThrow();

        docA1 = documentRepository.save(DocumentEntity.builder()
                .user(userA)
                .title("Family Health Plan 2024")
                .originalFilename("policy_2024.pdf")
                .storagePath("/storage/policy_2024.pdf")
                .mimeType("application/pdf")
                .fileSize(2048L)
                .category(DocumentCategory.INSURANCE)
                .checksumSha256("1".repeat(64))
                .extractedText("Family Health Plan 2024. Sum insured 500000 INR. Waiting period 36 months.")
                .build());

        docA2 = documentRepository.save(DocumentEntity.builder()
                .user(userA)
                .title("Family Health Plan 2025")
                .originalFilename("policy_2025.pdf")
                .storagePath("/storage/policy_2025.pdf")
                .mimeType("application/pdf")
                .fileSize(2100L)
                .category(DocumentCategory.INSURANCE)
                .checksumSha256("2".repeat(64))
                .extractedText("Family Health Plan 2025 Enhanced. Sum insured 1000000 INR. Waiting period 24 months.")
                .build());

        docB1 = documentRepository.save(DocumentEntity.builder()
                .user(userB)
                .title("User B Secret Life Policy")
                .originalFilename("userB_policy.pdf")
                .storagePath("/storage/userB_policy.pdf")
                .mimeType("application/pdf")
                .fileSize(1024L)
                .category(DocumentCategory.INSURANCE)
                .checksumSha256("3".repeat(64))
                .extractedText("User B Life coverage details.")
                .build());
    }

    @Test
    @DisplayName("POST /api/v1/insurance/policies/compare compares two policies and emits audit log")
    void comparePoliciesSuccessful() throws Exception {
        PolicyComparisonRequest request = PolicyComparisonRequest.builder()
                .documentId1(docA1.getId())
                .documentId2(docA2.getId())
                .build();

        mockMvc.perform(post("/api/v1/insurance/policies/compare")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentId1").value(docA1.getId().toString()))
                .andExpect(jsonPath("$.data.documentId2").value(docA2.getId().toString()))
                .andExpect(jsonPath("$.data.documentTitle1").value(docA1.getTitle()))
                .andExpect(jsonPath("$.data.documentTitle2").value(docA2.getTitle()))
                .andExpect(jsonPath("$.data.summary").isNotEmpty())
                .andExpect(jsonPath("$.data.clauseDifferences", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.addedBenefits", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.overallRecommendation").isNotEmpty());

        // Verify audit log
        boolean auditLogged = false;
        for (int i = 0; i < 25; i++) {
            auditLogged = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userAId, Pageable.unpaged())
                    .getContent().stream()
                    .anyMatch(a -> a.getEventType() == AuditEventType.POLICY_COMPARISON_EXECUTED);
            if (auditLogged) break;
            Thread.sleep(100);
        }
        assertThat(auditLogged).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/insurance/policies/compare prevents cross-tenant access to another user's document")
    void crossTenantComparisonReturnsNotFound() throws Exception {
        // User A attempts to compare their document with User B's document
        PolicyComparisonRequest request = PolicyComparisonRequest.builder()
                .documentId1(docA1.getId())
                .documentId2(docB1.getId())
                .build();

        mockMvc.perform(post("/api/v1/insurance/policies/compare")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/insurance/policies/compare unauthenticated returns 401")
    void unauthenticatedComparisonFails() throws Exception {
        PolicyComparisonRequest request = PolicyComparisonRequest.builder()
                .documentId1(docA1.getId())
                .documentId2(docA2.getId())
                .build();

        mockMvc.perform(post("/api/v1/insurance/policies/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
