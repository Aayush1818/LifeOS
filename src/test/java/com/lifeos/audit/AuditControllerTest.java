package com.lifeos.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.entity.AuditOutcome;
import com.lifeos.audit.service.AuditService;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("audit.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Audit")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("audit.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Audit")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs returns paginated events for current user")
    void getAuditLogsForCurrentUser() throws Exception {
        auditService.logEvent(userAId, AuditEventType.AUTH_LOGIN_SUCCESS, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{\"method\":\"test\"}");
        auditService.logEvent(userAId, AuditEventType.DATA_EXPORT_REQUESTED, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{\"items\":5}");

        mockMvc.perform(get("/api/v1/audit/logs")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.content[*].userId", everyItem(equalTo(userAId.toString()))));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs enforces multi-tenant isolation between users")
    void enforcesUserIsolation() throws Exception {
        auditService.logEvent(userAId, AuditEventType.POLICY_COMPARISON_EXECUTED, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{}");
        auditService.logEvent(userBId, AuditEventType.AUTH_LOGOUT, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{}");

        mockMvc.perform(get("/api/v1/audit/logs")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].userId", everyItem(equalTo(userAId.toString()))))
                .andExpect(jsonPath("$.data.content[*].userId", not(hasItem(userBId.toString()))));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs filters by eventType accurately")
    void filtersByEventType() throws Exception {
        auditService.logEvent(userAId, AuditEventType.ACTION_HITL_CONFIRMED, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{}");
        auditService.logEvent(userAId, AuditEventType.ACTION_HITL_REJECTED, AuditOutcome.SUCCESS, "127.0.0.1", "TestAgent", "{}");

        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("eventType", "ACTION_HITL_CONFIRMED")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].eventType", everyItem(equalTo("ACTION_HITL_CONFIRMED"))));
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs without authentication returns 401 Unauthorized")
    void unauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
