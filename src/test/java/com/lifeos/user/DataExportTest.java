package com.lifeos.user;

import com.lifeos.audit.entity.AuditEventType;
import com.lifeos.audit.repository.AuditLogRepository;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.entity.DependentEntity;
import com.lifeos.dependent.entity.RelationshipType;
import com.lifeos.dependent.repository.DependentRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataExportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DependentRepository dependentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;
    private String userAEmail;

    @BeforeEach
    void setUp() {
        userAEmail = ("export.usera." + UUID.randomUUID() + "@example.com").toLowerCase();
        RegisterRequest reqA = RegisterRequest.builder()
                .email(userAEmail)
                .password("Password123!@#")
                .firstName("Export")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("export.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Export")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("GET /api/v1/users/me/export returns comprehensive multi-domain data bundle and emits audit log")
    void fullDataExportSuccessful() throws Exception {
        UserEntity userA = userRepository.findById(userAId).orElseThrow();
        DependentEntity dep = DependentEntity.builder()
                .user(userA)
                .fullName("Little Timmy")
                .relationship(RelationshipType.CHILD)
                .dateOfBirth(LocalDate.of(2018, 4, 15))
                .emergencyPhone("+1112223333")
                .build();
        dependentRepository.save(dep);

        mockMvc.perform(get("/api/v1/users/me/export")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportVersion").value("1.0"))
                .andExpect(jsonPath("$.userId").value(userAId.toString()))
                .andExpect(jsonPath("$.userProfile.email").value(userAEmail))
                .andExpect(jsonPath("$.dependents", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.dependents[0].fullName").value("Little Timmy"))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.budgets").isArray())
                .andExpect(jsonPath("$.loans").isArray())
                .andExpect(jsonPath("$.insurancePolicies").isArray())
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.trips").isArray())
                .andExpect(jsonPath("$.assets").isArray())
                .andExpect(jsonPath("$.reminders").isArray())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.documents").isArray())
                .andExpect(jsonPath("$.conversations").isArray());

        // Verify audit event persisted
        boolean auditLogged = false;
        for (int i = 0; i < 25; i++) {
            auditLogged = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userAId, org.springframework.data.domain.Pageable.unpaged())
                    .getContent().stream()
                    .anyMatch(a -> a.getEventType() == AuditEventType.DATA_EXPORT_REQUESTED);
            if (auditLogged) break;
            Thread.sleep(100);
        }
        assertThat(auditLogged).isTrue();
    }

    @Test
    @DisplayName("GET /api/v1/users/me/export enforces user isolation")
    void userIsolationInExport() throws Exception {
        UserEntity userA = userRepository.findById(userAId).orElseThrow();
        DependentEntity dep = DependentEntity.builder()
                .user(userA)
                .fullName("Exclusive Secret Dependent")
                .relationship(RelationshipType.PARENT)
                .dateOfBirth(LocalDate.of(1960, 1, 1))
                .build();
        dependentRepository.save(dep);

        // User B exports their data, should NOT contain User A's dependent
        mockMvc.perform(get("/api/v1/users/me/export")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userBId.toString()))
                .andExpect(jsonPath("$.dependents", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/users/me/export unauthenticated returns 401")
    void unauthenticatedExportFails() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/export")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
