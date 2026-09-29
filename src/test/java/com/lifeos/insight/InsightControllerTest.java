package com.lifeos.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.insight.repository.InsightRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InsightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InsightRepository insightRepository;

    private String userToken;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        RegisterRequest req = RegisterRequest.builder()
                .email("ctrl.insight." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Ctrl")
                .lastName("Insight")
                .build();
        AuthResponse res = authService.register(req);
        userToken = res.getAccessToken();
        user = userRepository.findById(res.getUser().getId()).orElseThrow();
    }

    @Test
    @DisplayName("GET /api/v1/insights returns active insights with severity metrics")
    void getActiveInsightsSuccess() throws Exception {
        // Pre-create an insight
        InsightEntity entity = InsightEntity.builder()
                .user(user)
                .insightType(InsightType.SPENDING_SURGE)
                .severity(InsightSeverity.WARNING)
                .title("Elevated Food Spend")
                .description("Food spending increased significantly this week.")
                .actionType(InsightActionType.VIEW_BUDGET)
                .actionPayload(Map.of("category", "FOOD_DINING"))
                .build();
        insightRepository.save(entity);

        mockMvc.perform(get("/api/v1/insights")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActive").isNumber())
                .andExpect(jsonPath("$.data.warningCount").value(1))
                .andExpect(jsonPath("$.data.insights[0].title").value("Elevated Food Spend"));
    }

    @Test
    @DisplayName("POST /api/v1/insights/{id}/dismiss and /action successfully update state")
    void dismissAndActionInsight() throws Exception {
        InsightEntity entity = InsightEntity.builder()
                .user(user)
                .insightType(InsightType.HIGH_INTEREST_LOAN)
                .severity(InsightSeverity.INFO)
                .title("High APR Alert")
                .description("Loan APR is above 8%.")
                .actionType(InsightActionType.SIMULATE_PREPAYMENT)
                .build();
        entity = insightRepository.save(entity);

        // 1. Action the insight
        mockMvc.perform(post("/api/v1/insights/" + entity.getId() + "/action")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.actioned").value(true));

        // 2. Dismiss the insight
        mockMvc.perform(post("/api/v1/insights/" + entity.getId() + "/dismiss")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dismissed").value(true));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/insights"))
                .andExpect(status().isUnauthorized());
    }
}
