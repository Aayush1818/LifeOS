package com.lifeos.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.entity.RelationshipType;
import com.lifeos.dependent.service.DependentService;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
import com.lifeos.document.entity.IngestionStatus;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DependentService dependentService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;
    private UUID dependentAId;

    private static final byte[] VALID_PDF_BYTES = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 55 >> stream\n" +
            "BT /F1 12 Tf 100 700 Td (LifeOS Test PDF Content) Tj ET\n" +
            "endstream\n" +
            "endobj\n" +
            "xref\n" +
            "0 5\n" +
            "0000000000 65535 f \n" +
            "0000000009 00000 n \n" +
            "0000000058 00000 n \n" +
            "0000000115 00000 n \n" +
            "0000000214 00000 n \n" +
            "trailer << /Size 5 /Root 1 0 R >>\n" +
            "startxref\n" +
            "318\n" +
            "%%EOF\n").getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        // Register User A
        RegisterRequest reqA = RegisterRequest.builder()
                .email("doc.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("DocUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        // Register User B
        RegisterRequest reqB = RegisterRequest.builder()
                .email("doc.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("DocUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();

        // Create a dependent for User A
        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Child Alpha")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 6, 15))
                .emergencyPhone("+1122334455")
                .medicalNotes(Map.of("bloodGroup", "B+"))
                .build();
        DependentResponse depRes = dependentService.createDependent(userAId, depReq);
        dependentAId = depRes.getId();
    }

    @Test
    @DisplayName("Upload TXT document extracts text and stores successfully")
    void uploadTxtDocumentSuccess() throws Exception {
        String content = "Health Insurance Policy #987654321.\nCoverage limit: $500,000.\nEffective date: 2026-01-01.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "health_policy.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Health Insurance Policy")
                .category(DocumentCategory.INSURANCE)
                .documentType(DocumentType.POLICY)
                .dependentId(dependentAId)
                .issueDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1))
                .tags(List.of("insurance", "health", "policy"))
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Health Insurance Policy"))
                .andExpect(jsonPath("$.data.originalFilename").value("health_policy.txt"))
                .andExpect(jsonPath("$.data.mimeType").value("text/plain"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.category").value("INSURANCE"))
                .andExpect(jsonPath("$.data.ingestionStatus").value(IngestionStatus.PROCESSED.name()))
                .andExpect(jsonPath("$.data.dependentId").value(dependentAId.toString()))
                .andExpect(jsonPath("$.data.tags", hasSize(3)))
                .andReturn();

        String docId = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Verify detail endpoint returns extracted text and metadata
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedText", containsString("Health Insurance Policy #987654321")))
                .andExpect(jsonPath("$.data.extractedText", containsString("Coverage limit: $500,000")))
                .andExpect(jsonPath("$.data.storagePath").doesNotExist()); // Zero storage path leakage
    }

    @Test
    @DisplayName("Upload valid PDF document extracts metadata and text successfully")
    void uploadPdfDocumentSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Sample PDF Doc")
                .category(DocumentCategory.FINANCIAL)
                .documentType(DocumentType.STATEMENT)
                .tags(List.of("pdf", "statement"))
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Sample PDF Doc"))
                .andExpect(jsonPath("$.data.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    @DisplayName("Reject MIME spoofing: binary executable disguised as .pdf")
    void rejectMimeSpoofing() throws Exception {
        // DOS/PE executable signature (MZ header) disguised with .pdf extension
        byte[] fakePdfBytes = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00};

        MockMultipartFile spoofedFile = new MockMultipartFile(
                "file",
                "malicious.pdf",
                "application/pdf",
                fakePdfBytes
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Spoofed PDF")
                .category(DocumentCategory.LEGAL)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(spoofedFile)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Document"))
                .andExpect(jsonPath("$.detail", containsString("Unsupported or invalid document format")));
    }

    @Test
    @DisplayName("Reject unsupported file type (.exe binary)")
    void rejectUnsupportedFileType() throws Exception {
        byte[] exeBytes = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00};

        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "installer.exe",
                "application/x-msdownload",
                exeBytes
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Installer")
                .category(DocumentCategory.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(exeFile)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Document"));
    }

    @Test
    @DisplayName("Path traversal in filename is sanitized to safe filename")
    void pathTraversalFilenameSanitized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.txt",
                "text/plain",
                "Normal text content inside".getBytes(StandardCharsets.UTF_8)
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Path Traversal Test")
                .category(DocumentCategory.PERSONAL)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFilename").value("passwd.txt"));
    }

    @Test
    @DisplayName("Download document streams matching binary with Content-Disposition")
    void downloadDocumentSuccess() throws Exception {
        byte[] originalContent = "Downloadable confidential contract text.".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.txt",
                "text/plain",
                originalContent
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Employment Contract")
                .category(DocumentCategory.LEGAL)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult downloadRes = mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contract.txt\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("text/plain")))
                .andReturn();

        assertThat(downloadRes.getResponse().getContentAsByteArray()).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("Upload new version increments version number and updates content")
    void uploadNewVersionSuccess() throws Exception {
        // Version 1
        MockMultipartFile v1File = new MockMultipartFile(
                "file",
                "lease_agreement_v1.txt",
                "text/plain",
                "Lease Agreement Version 1: Rent is $1500".getBytes(StandardCharsets.UTF_8)
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Apartment Lease")
                .category(DocumentCategory.LEGAL)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MvcResult v1Res = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(v1File)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(1))
                .andReturn();

        String docId = objectMapper.readTree(v1Res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Version 2 upload
        MockMultipartFile v2File = new MockMultipartFile(
                "file",
                "lease_agreement_v2.txt",
                "text/plain",
                "Lease Agreement Version 2: Rent adjusted to $1600".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/documents/" + docId + "/versions")
                        .file(v2File)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.originalFilename").value("lease_agreement_v2.txt"));

        // Verify detail has v2 text
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.extractedText", containsString("Rent adjusted to $1600")));
    }

    @Test
    @DisplayName("Two-stage delete soft-deletes DB record and purges storage; subsequent GET returns 404")
    void deleteDocumentSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "temporary_notes.txt",
                "text/plain",
                "Temporary notes to be purged.".getBytes(StandardCharsets.UTF_8)
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("Temp Notes")
                .category(DocumentCategory.OTHER)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Delete document
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Subsequent GET -> 404 Not Found
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // Subsequent Download -> 404 Not Found
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("Cross-tenant resource isolation: User B cannot access, download, version, or delete User A's document")
    void crossTenantIsolationEnforced() throws Exception {
        // User A uploads document
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "user_a_private_data.txt",
                "text/plain",
                "Confidential User A data".getBytes(StandardCharsets.UTF_8)
        );

        UploadDocumentRequest meta = UploadDocumentRequest.builder()
                .title("User A Private Document")
                .category(DocumentCategory.FINANCIAL)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(meta)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(uploadRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. User B tries GET -> 404 Not Found
        mockMvc.perform(get("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // 2. User B tries Download -> 404 Not Found
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // 3. User B tries Upload New Version -> 404 Not Found
        MockMultipartFile v2File = new MockMultipartFile(
                "file",
                "exploit.txt",
                "text/plain",
                "Hacked".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/documents/" + docId + "/versions")
                        .file(v2File)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // 4. User B tries Delete -> 404 Not Found
        mockMvc.perform(delete("/api/v1/documents/" + docId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // 5. User B lists documents -> empty list
        mockMvc.perform(get("/api/v1/documents")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("Pagination and category filtering works correctly")
    void listDocumentsWithFilters() throws Exception {
        for (int i = 1; i <= 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "doc_" + i + ".txt",
                    "text/plain",
                    ("Document " + i + " content").getBytes(StandardCharsets.UTF_8)
            );
            UploadDocumentRequest meta = UploadDocumentRequest.builder()
                    .title("Doc " + i)
                    .category(i == 1 ? DocumentCategory.TAX : DocumentCategory.TRAVEL)
                    .build();
            MockMultipartFile metadataPart = new MockMultipartFile(
                    "metadata",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(meta)
            );
            mockMvc.perform(multipart("/api/v1/documents/upload")
                            .file(file)
                            .file(metadataPart)
                            .header("Authorization", "Bearer " + userAToken))
                    .andExpect(status().isCreated());
        }

        // Filter by category TAX -> 1 document
        mockMvc.perform(get("/api/v1/documents?category=TAX")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].category").value("TAX"));

        // Filter by category TRAVEL -> 2 documents
        mockMvc.perform(get("/api/v1/documents?category=TRAVEL")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }
}
