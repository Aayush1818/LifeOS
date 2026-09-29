package com.lifeos.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.reminder.dto.CreateReminderRequest;
import com.lifeos.reminder.dto.UpdateReminderRequest;
import com.lifeos.reminder.entity.ReminderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReminderControllerTest {

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
        RegisterRequest reqA = RegisterRequest.builder()
                .email("reminder.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("ReminderA")
                .lastName("User")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("reminder.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("ReminderB")
                .lastName("User")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
    }

    @Test
    @DisplayName("Should create and list reminders with status transitions")
    void testReminderLifecycle() throws Exception {
        OffsetDateTime due = OffsetDateTime.now().plusDays(2);
        CreateReminderRequest createReq = CreateReminderRequest.builder()
                .title("Renew passport")
                .description("Expiring next month")
                .dueAt(due)
                .recurrencePattern("MONTHLY")
                .reminderType("PERSONAL")
                .build();

        // 1. Create reminder
        MvcResult createResult = mockMvc.perform(post("/api/v1/reminders")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Renew passport")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(responseBody).path("data").path("id").asText();

        // 2. List reminders
        mockMvc.perform(get("/api/v1/reminders")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));

        // 3. Get upcoming reminders
        mockMvc.perform(get("/api/v1/reminders/upcoming?windowDays=7")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // 4. Update reminder
        UpdateReminderRequest updateReq = UpdateReminderRequest.builder()
                .title("Renew passport updated")
                .build();
        mockMvc.perform(put("/api/v1/reminders/" + reminderId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title", is("Renew passport updated")));

        // 5. Complete reminder (should schedule next monthly occurrence)
        mockMvc.perform(patch("/api/v1/reminders/" + reminderId + "/complete")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("COMPLETED")));

        // 6. Verify User B cannot access User A's reminder (Tenant isolation -> 404)
        mockMvc.perform(get("/api/v1/reminders/" + reminderId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 7. Delete reminder
        mockMvc.perform(delete("/api/v1/reminders/" + reminderId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());
    }
}
