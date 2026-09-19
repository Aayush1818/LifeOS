package com.lifeos.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.LoginRequest;
import com.lifeos.auth.dto.RefreshTokenRequest;
import com.lifeos.auth.dto.RegisterRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String UNIQUE_EMAIL = "auth.test." + UUID.randomUUID() + "@example.com";
    private static final String PASSWORD = "StrongPassword123!";
    private static String refreshToken;

    @Test
    @Order(1)
    void registerShouldSucceedWithValidPayload() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(UNIQUE_EMAIL)
                .password(PASSWORD)
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(UNIQUE_EMAIL))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        refreshToken = objectMapper.readTree(responseContent).get("data").get("refreshToken").asText();
    }

    @Test
    @Order(2)
    void registerShouldFailWithDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(UNIQUE_EMAIL)
                .password(PASSWORD)
                .firstName("Another")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("already registered")));
    }

    @Test
    @Order(3)
    void registerShouldFailWithWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("weak.pwd." + UUID.randomUUID() + "@example.com")
                .password("weak")
                .firstName("Weak")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));
    }

    @Test
    @Order(4)
    void loginShouldSucceedWithCorrectCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(UNIQUE_EMAIL)
                .password(PASSWORD)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(UNIQUE_EMAIL));
    }

    @Test
    @Order(5)
    void loginShouldFailWithIncorrectPassword() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(UNIQUE_EMAIL)
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail", containsString("Invalid email or password")));
    }

    @Test
    @Order(6)
    void refreshTokenShouldSucceedAndRotateToken() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(not(refreshToken))) // Must rotate!
                .andReturn();

        // Attempting to reuse the old refresh token must fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void logoutShouldRevokeRefreshToken() throws Exception {
        // Register a new user to test logout revocation
        String uniqueEmail = "logout_" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(uniqueEmail)
                .password(PASSWORD)
                .firstName("Logout")
                .lastName("Test")
                .phone("+1234567890")
                .build();

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String regContent = regResult.getResponse().getContentAsString();
        String tokenToRevoke = objectMapper.readTree(regContent).get("data").get("refreshToken").asText();

        RefreshTokenRequest logoutReq = RefreshTokenRequest.builder()
                .refreshToken(tokenToRevoke)
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Now refresh with this revoked token must fail with 403
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutReq)))
                .andExpect(status().isForbidden());
    }
}
