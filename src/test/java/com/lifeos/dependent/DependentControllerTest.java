package com.lifeos.dependent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.UpdateDependentRequest;
import com.lifeos.dependent.entity.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DependentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        // Register User A
        RegisterRequest userARequest = RegisterRequest.builder()
                .email("userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("User")
                .lastName("Alpha")
                .build();
        AuthResponse userAResponse = authService.register(userARequest);
        userAToken = userAResponse.getAccessToken();

        // Register User B
        RegisterRequest userBRequest = RegisterRequest.builder()
                .email("userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("User")
                .lastName("Beta")
                .build();
        AuthResponse userBResponse = authService.register(userBRequest);
        userBToken = userBResponse.getAccessToken();
    }

    @Test
    void dependentCrudAndResourceIsolationTest() throws Exception {
        // 1. User A creates a dependent
        CreateDependentRequest createRequest = CreateDependentRequest.builder()
                .fullName("Jane Alpha")
                .relationship(RelationshipType.SPOUSE)
                .dateOfBirth(LocalDate.of(1992, 5, 14))
                .emergencyPhone("+1987654321")
                .medicalNotes(Map.of("bloodGroup", "O+", "allergies", "Penicillin"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/dependents")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Jane Alpha"))
                .andExpect(jsonPath("$.data.relationship").value("SPOUSE"))
                .andReturn();

        String createContent = createResult.getResponse().getContentAsString();
        String dependentId = objectMapper.readTree(createContent).get("data").get("id").asText();

        // 2. User A can list their dependent
        mockMvc.perform(get("/api/v1/dependents")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(dependentId));

        // 3. User B lists dependents -> should be empty (User B cannot see User A's dependent)
        mockMvc.perform(get("/api/v1/dependents")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        // 4. RESOURCE ISOLATION ENFORCEMENT:
        // User B attempts to access User A's dependent by ID -> Must return 404 Not Found!
        mockMvc.perform(get("/api/v1/dependents/" + dependentId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        // User B attempts to update User A's dependent -> Must return 404 Not Found!
        UpdateDependentRequest maliciousUpdate = UpdateDependentRequest.builder()
                .fullName("Hacked Name")
                .relationship(RelationshipType.OTHER)
                .build();

        mockMvc.perform(put("/api/v1/dependents/" + dependentId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isNotFound());

        // User B attempts to delete User A's dependent -> Must return 404 Not Found!
        mockMvc.perform(delete("/api/v1/dependents/" + dependentId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 5. User A updates their dependent
        UpdateDependentRequest validUpdate = UpdateDependentRequest.builder()
                .fullName("Jane Alpha-Smith")
                .relationship(RelationshipType.SPOUSE)
                .dateOfBirth(LocalDate.of(1992, 5, 14))
                .emergencyPhone("+1999888777")
                .medicalNotes(Map.of("bloodGroup", "O+", "allergies", "Penicillin, Peanuts"))
                .build();

        mockMvc.perform(put("/api/v1/dependents/" + dependentId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Jane Alpha-Smith"))
                .andExpect(jsonPath("$.data.emergencyPhone").value("+1999888777"));

        // 6. User A soft-deletes their dependent
        mockMvc.perform(delete("/api/v1/dependents/" + dependentId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // 7. User A lists dependents -> should be empty after soft-delete
        mockMvc.perform(get("/api/v1/dependents")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
