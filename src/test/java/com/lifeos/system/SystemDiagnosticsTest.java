package com.lifeos.system;

import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemDiagnosticsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    private String userToken;

    @BeforeEach
    void setUp() {
        RegisterRequest req = RegisterRequest.builder()
                .email("system.diag." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Sys")
                .lastName("Admin")
                .build();
        AuthResponse res = authService.register(req);
        userToken = res.getAccessToken();
    }

    @Test
    @DisplayName("GET /api/v1/system/health returns live component health probes without authentication")
    void getSystemHealthPublic() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status", notNullValue()))
                .andExpect(jsonPath("$.data.components.database.status").value("UP"))
                .andExpect(jsonPath("$.data.components.database.latencyMs", notNullValue()))
                .andExpect(jsonPath("$.data.components.pgvector.status", notNullValue()))
                .andExpect(jsonPath("$.data.components.documentStorage.status", notNullValue()))
                .andExpect(jsonPath("$.data.components.apacheTika.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/system/metrics returns platform and JVM metrics when authenticated")
    void getSystemMetricsAuthenticated() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(get("/api/v1/system/metrics")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(res.getResponse().getContentAsString()).get("data");
        org.assertj.core.api.Assertions.assertThat(root.get("totalUsers").asLong()).isGreaterThanOrEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(root.get("jvmAvailableProcessors").asInt()).isGreaterThan(0);
        org.assertj.core.api.Assertions.assertThat(root.get("diskFreeBytes").asLong()).isGreaterThan(0L);
        org.assertj.core.api.Assertions.assertThat(root.get("diskTotalBytes").asLong()).isGreaterThan(0L);
        org.assertj.core.api.Assertions.assertThat(root.get("jvmUsedMemoryBytes").asLong()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("GET /api/v1/system/metrics without authentication returns 401 Unauthorized")
    void getSystemMetricsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/system/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
