package com.lifeos.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private String accessToken;
    private String userEmail;

    @BeforeEach
    void setUp() {
        userEmail = "user.profile." + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(userEmail)
                .password("Password123!@#")
                .firstName("Alice")
                .lastName("Smith")
                .phone("+9876543210")
                .build();

        AuthResponse authResponse = authService.register(registerRequest);
        accessToken = authResponse.getAccessToken();
    }

    @Test
    void getProfileWithoutTokenShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void getProfileWithValidTokenShouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(userEmail))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"));
    }

    @Test
    void updateProfileShouldPersistChanges() throws Exception {
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Alicia")
                .lastName("Keys")
                .phone("+1122334455")
                .preferences(Map.of("theme", "dark", "currency", "USD"))
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alicia"))
                .andExpect(jsonPath("$.data.lastName").value("Keys"))
                .andExpect(jsonPath("$.data.phone").value("+1122334455"))
                .andExpect(jsonPath("$.data.preferences.theme").value("dark"));
    }
}
