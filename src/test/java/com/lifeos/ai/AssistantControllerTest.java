package com.lifeos.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.dto.CreateConversationRequest;
import com.lifeos.ai.dto.SendMessageRequest;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.document.dto.UploadDocumentRequest;
import com.lifeos.document.entity.DocumentCategory;
import com.lifeos.document.entity.DocumentType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DocumentService documentService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    private static final byte[] SAMPLE_DOC_PDF = ("%PDF-1.4\n" +
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >> endobj\n" +
            "4 0 obj << /Length 120 >> stream\n" +
            "BT /F1 12 Tf 72 712 Td (SECTION 1: COVERAGE AND BENEFITS The comprehensive medical deductible is $1000 per policy year.) ET\n" +
            "endstream endobj\n" +
            "xref\n0 5\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000214 00000 n \n" +
            "trailer << /Size 5 /Root 1 0 R >>\nstartxref\n386\n%%EOF").getBytes(StandardCharsets.US_ASCII);

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("assistant.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("AssistantUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("assistant.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("AssistantUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    private String createConversationForUserA(String title) throws Exception {
        CreateConversationRequest req = new CreateConversationRequest(title);
        String responseBody = mockMvc.perform(post("/api/v1/assistant/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseBody).path("data").path("id").asText();
    }

    @Test
    @DisplayName("Should create conversation successfully for authenticated user")
    void shouldCreateConversation() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest("Insurance Planning");

        mockMvc.perform(post("/api/v1/assistant/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Insurance Planning")))
                .andExpect(jsonPath("$.data.id", notNullValue()));
    }

    @Test
    @DisplayName("Should list user conversations with pagination")
    void shouldListConversations() throws Exception {
        createConversationForUserA("Conv 1");
        createConversationForUserA("Conv 2");

        mockMvc.perform(get("/api/v1/assistant/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Should get conversation details and messages")
    void shouldGetConversationDetails() throws Exception {
        String convId = createConversationForUserA("Review Session");

        mockMvc.perform(get("/api/v1/assistant/conversations/{id}", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(convId)))
                .andExpect(jsonPath("$.data.title", is("Review Session")));
    }

    @Test
    @DisplayName("Should return 404 when User B tries to access User A's conversation")
    void shouldEnforceTenantIsolationOnGet() throws Exception {
        String convId = createConversationForUserA("Private Conversation");

        mockMvc.perform(get("/api/v1/assistant/conversations/{id}", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Should send message, retrieve context, and return grounded response")
    void shouldSendMessageAndReceiveGroundedResponse() throws Exception {
        // Upload policy for User A
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Health_Insurance_Policy.pdf",
                "application/pdf",
                SAMPLE_DOC_PDF
        );
        UploadDocumentRequest docReq = UploadDocumentRequest.builder()
                .title("Health Insurance Policy 2026")
                .category(DocumentCategory.INSURANCE)
                .documentType(DocumentType.POLICY)
                .build();
        documentService.uploadDocument(file, docReq, userAId);

        String convId = createConversationForUserA("Policy QA");

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .content("What is my deductible amount?")
                .build();

        mockMvc.perform(post("/api/v1/assistant/conversations/{id}/messages", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.conversationId", is(convId)))
                .andExpect(jsonPath("$.data.userMessage.content", is("What is my deductible amount?")))
                .andExpect(jsonPath("$.data.assistantMessage.role", is("ASSISTANT")))
                .andExpect(jsonPath("$.data.assistantMessage.content", notNullValue()))
                .andExpect(jsonPath("$.data.modelMetadata.provider", is("MOCK")));
    }

    @Test
    @DisplayName("Should return 404 when User B tries to send message to User A's conversation")
    void shouldEnforceTenantIsolationOnSendMessage() throws Exception {
        String convId = createConversationForUserA("User A Chat");

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .content("Attempting cross-tenant access")
                .build();

        mockMvc.perform(post("/api/v1/assistant/conversations/{id}/messages", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Should delete conversation and return 404 on subsequent get")
    void shouldDeleteConversation() throws Exception {
        String convId = createConversationForUserA("To Be Deleted");

        mockMvc.perform(delete("/api/v1/assistant/conversations/{id}", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(get("/api/v1/assistant/conversations/{id}", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when User B tries to delete User A's conversation")
    void shouldEnforceTenantIsolationOnDelete() throws Exception {
        String convId = createConversationForUserA("Cannot Delete This");

        mockMvc.perform(delete("/api/v1/assistant/conversations/{id}", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when message content is blank")
    void shouldRejectBlankMessage() throws Exception {
        String convId = createConversationForUserA("Validation Test");

        SendMessageRequest msgReq = SendMessageRequest.builder()
                .content("   ")
                .build();

        mockMvc.perform(post("/api/v1/assistant/conversations/{id}/messages", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isBadRequest());
    }
}
