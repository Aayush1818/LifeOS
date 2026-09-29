package com.lifeos.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.reminder.dto.NotificationResponse;
import com.lifeos.reminder.entity.NotificationChannel;
import com.lifeos.reminder.entity.NotificationType;
import com.lifeos.reminder.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private NotificationService notificationService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("notif.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("NotifA")
                .lastName("User")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("notif.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("NotifB")
                .lastName("User")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("Should retrieve notifications, unread count, and mark as read")
    void testNotificationOperations() throws Exception {
        // Dispatch test notification to User A
        NotificationResponse notif = notificationService.dispatchNotification(
                userAId,
                "Payment Due Alert",
                "Your loan installment of $500 is due tomorrow",
                NotificationChannel.IN_APP,
                NotificationType.LOAN_EMI,
                UUID.randomUUID(),
                Map.of("amount", 500)
        );

        // 1. Check unread count for User A
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(1)));

        // 2. User B should have 0 unread
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(0)));

        // 3. User A lists notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title", is("Payment Due Alert")))
                .andExpect(jsonPath("$.data.content[0].read", is(false)));

        // 4. User B cannot mark User A's notification as read (Tenant isolation -> 404)
        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 5. User A marks notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read", is(true)));

        // 6. Check unread count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", is(0)));

        // 7. Test read-all
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());
    }
}
