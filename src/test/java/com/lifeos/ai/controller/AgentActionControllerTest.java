package com.lifeos.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.agent.dto.PendingActionDto;
import com.lifeos.ai.agent.service.ActionConfirmationService;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentActionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private ActionConfirmationService actionConfirmationService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        RegisterRequest regA = RegisterRequest.builder()
                .email("agentuser_a_" + uniqueSuffix + "@lifeos.io")
                .password("Password@123")
                .firstName("Alice")
                .lastName("Agent")
                .build();
        AuthResponse respA = authService.register(regA);
        userAToken = "Bearer " + respA.getAccessToken();
        userAId = respA.getUser().getId();

        RegisterRequest regB = RegisterRequest.builder()
                .email("agentuser_b_" + uniqueSuffix + "@lifeos.io")
                .password("Password@123")
                .firstName("Bob")
                .lastName("Agent")
                .build();
        AuthResponse respB = authService.register(regB);
        userBToken = "Bearer " + respB.getAccessToken();
        userBId = respB.getUser().getId();
    }

    @Test
    @DisplayName("GET /pending should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/assistant/actions/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /pending should return list of pending actions for authenticated user")
    void shouldReturnPendingActionsForUser() throws Exception {
        // Create pending action for user A
        actionConfirmationService.createPendingAction(
                userAId, null, "create_reminder",
                Map.of("title", "Review Insurance Policy", "dueAt", "2026-10-15T10:00:00Z"),
                "Schedule a reminder for Review Insurance Policy?");

        mockMvc.perform(get("/api/v1/assistant/actions/pending")
                        .header(HttpHeaders.AUTHORIZATION, userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].toolName", is("create_reminder")))
                .andExpect(jsonPath("$.data[0].status", is("PENDING")));

        // User B should see empty list (tenant isolation)
        mockMvc.perform(get("/api/v1/assistant/actions/pending")
                        .header(HttpHeaders.AUTHORIZATION, userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("POST /{id}/confirm should execute tool and update status to CONFIRMED")
    void shouldConfirmActionSuccessfully() throws Exception {
        PendingActionDto pending = actionConfirmationService.createPendingAction(
                userAId, null, "create_reminder",
                Map.of("title", "Annual Dentist Appointment", "dueAt", "2026-11-01T09:00:00Z"),
                "Schedule a reminder for Annual Dentist Appointment?");

        mockMvc.perform(post("/api/v1/assistant/actions/{id}/confirm", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.data.success", is(true)))
                .andExpect(jsonPath("$.data.message", is("Action executed successfully")));
    }

    @Test
    @DisplayName("POST /{id}/reject should cancel action without executing tool")
    void shouldRejectActionSuccessfully() throws Exception {
        PendingActionDto pending = actionConfirmationService.createPendingAction(
                userAId, null, "create_reminder",
                Map.of("title", "Cancelled Task", "dueAt", "2026-11-01T09:00:00Z"),
                "Schedule a reminder for Cancelled Task?");

        mockMvc.perform(post("/api/v1/assistant/actions/{id}/reject", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REJECTED")))
                .andExpect(jsonPath("$.data.message", containsString("cancelled")));
    }

    @Test
    @DisplayName("Cross-tenant protection: User B cannot confirm User A's pending action (404)")
    void shouldPreventCrossTenantConfirmation() throws Exception {
        PendingActionDto pending = actionConfirmationService.createPendingAction(
                userAId, null, "create_reminder",
                Map.of("title", "Private Task", "dueAt", "2026-11-01T09:00:00Z"),
                "Schedule a reminder for Private Task?");

        // User B attempts to confirm User A's action
        mockMvc.perform(post("/api/v1/assistant/actions/{id}/confirm", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", containsString("Pending action not found")));

        // User B attempts to reject User A's action
        mockMvc.perform(post("/api/v1/assistant/actions/{id}/reject", pending.getId())
                        .header(HttpHeaders.AUTHORIZATION, userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", containsString("Pending action not found")));
    }
}
