package com.lifeos.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.dependent.dto.CreateDependentRequest;
import com.lifeos.dependent.dto.DependentResponse;
import com.lifeos.dependent.entity.RelationshipType;
import com.lifeos.dependent.service.DependentService;
import com.lifeos.insurance.dto.CreateInsuranceRequest;
import com.lifeos.insurance.dto.RenewPolicyRequest;
import com.lifeos.insurance.dto.UpdateInsuranceRequest;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.entity.PremiumFrequency;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.entity.ReminderStatus;
import com.lifeos.reminder.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InsuranceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private DependentService dependentService;

    @Autowired
    private ReminderRepository reminderRepository;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;
    private UUID dependentAId;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("ins.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("InsUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("ins.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("InsUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();

        CreateDependentRequest depReq = CreateDependentRequest.builder()
                .fullName("Spouse Alpha")
                .relationship(RelationshipType.SPOUSE)
                .dateOfBirth(LocalDate.of(1992, 5, 20))
                .emergencyPhone("+1987654321")
                .build();
        DependentResponse depRes = dependentService.createDependent(userAId, depReq);
        dependentAId = depRes.getId();
    }

    @Test
    @DisplayName("Create policy stores details and synchronizes renewal reminder automatically")
    void createPolicyAndVerifyReminderSync() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate expiry = today.plusYears(1);

        CreateInsuranceRequest request = CreateInsuranceRequest.builder()
                .policyNumber("POL-HEALTH-1001")
                .policyName("Comprehensive Family Health Shield")
                .providerName("Star Health Insurance")
                .policyType(InsuranceType.HEALTH)
                .coverageAmount(new BigDecimal("500000.00"))
                .premiumAmount(new BigDecimal("650.00"))
                .premiumFrequency(PremiumFrequency.MONTHLY)
                .startDate(today)
                .expiryDate(expiry)
                .nextRenewalDate(expiry)
                .dependentId(dependentAId)
                .notes("Family floater policy including spouse")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.policyNumber").value("POL-HEALTH-1001"))
                .andExpect(jsonPath("$.data.policyName").value("Comprehensive Family Health Shield"))
                .andExpect(jsonPath("$.data.coverageAmount").value(500000.00))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.dependentId").value(dependentAId.toString()))
                .andReturn();

        String policyId = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();

        // Verify reminder was automatically created in reminders table
        Optional<ReminderEntity> reminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(UUID.fromString(policyId), "INSURANCE_RENEWAL");
        assertThat(reminder).isPresent();
        assertThat(reminder.get().getStatus()).isEqualTo(ReminderStatus.ACTIVE);
        assertThat(reminder.get().getTitle()).contains("Comprehensive Family Health Shield");
        assertThat(reminder.get().getDueAt().toLocalDate()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("Duplicate policy number is rejected with 409 Conflict")
    void duplicatePolicyNumberRejected() throws Exception {
        LocalDate today = LocalDate.now();
        CreateInsuranceRequest request = CreateInsuranceRequest.builder()
                .policyNumber("DUPLICATE-POL-01")
                .providerName("General Life")
                .policyType(InsuranceType.LIFE)
                .coverageAmount(new BigDecimal("1000000.00"))
                .premiumAmount(new BigDecimal("1200.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today)
                .expiryDate(today.plusYears(10))
                .nextRenewalDate(today.plusYears(1))
                .build();

        mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate submission
        mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Renew policy advances expiry and updates renewal reminder date")
    void renewPolicyAdvancesReminder() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate initialExpiry = today.plusDays(10);

        CreateInsuranceRequest createReq = CreateInsuranceRequest.builder()
                .policyNumber("AUTO-RENEW-01")
                .policyName("Vehicle Comprehensive")
                .providerName("Motor Secure")
                .policyType(InsuranceType.VEHICLE)
                .coverageAmount(new BigDecimal("45000.00"))
                .premiumAmount(new BigDecimal("800.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today.minusYears(1))
                .expiryDate(initialExpiry)
                .nextRenewalDate(initialExpiry)
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String policyId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // Renew policy to 1 year beyond initial expiry
        LocalDate newExpiry = initialExpiry.plusYears(1);
        RenewPolicyRequest renewReq = RenewPolicyRequest.builder()
                .newExpiryDate(newExpiry)
                .newRenewalDate(newExpiry)
                .newPremiumAmount(new BigDecimal("850.00"))
                .notes("Annual renewal with 10% no-claim discount")
                .build();

        mockMvc.perform(post("/api/v1/insurance/" + policyId + "/renew")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiryDate").value(newExpiry.toString()))
                .andExpect(jsonPath("$.data.premiumAmount").value(850.00));

        // Verify reminder dueAt is advanced
        Optional<ReminderEntity> reminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(UUID.fromString(policyId), "INSURANCE_RENEWAL");
        assertThat(reminder).isPresent();
        assertThat(reminder.get().getDueAt().toLocalDate()).isEqualTo(newExpiry);
    }

    @Test
    @DisplayName("Upcoming renewals endpoint detects policies due within window")
    void getUpcomingRenewals() throws Exception {
        LocalDate today = LocalDate.now();

        // Policy 1: due in 15 days (within 30-day window)
        CreateInsuranceRequest p1 = CreateInsuranceRequest.builder()
                .policyNumber("DUE-SOON-01")
                .providerName("Travel Shield")
                .policyType(InsuranceType.TRAVEL)
                .coverageAmount(new BigDecimal("100000.00"))
                .premiumAmount(new BigDecimal("150.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today.minusMonths(6))
                .expiryDate(today.plusDays(15))
                .nextRenewalDate(today.plusDays(15))
                .build();

        mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isCreated());

        // Policy 2: due in 90 days (outside 30-day window)
        CreateInsuranceRequest p2 = CreateInsuranceRequest.builder()
                .policyNumber("DUE-LATER-01")
                .providerName("Home Guard")
                .policyType(InsuranceType.HOME_PROPERTY)
                .coverageAmount(new BigDecimal("300000.00"))
                .premiumAmount(new BigDecimal("400.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today.minusMonths(3))
                .expiryDate(today.plusDays(90))
                .nextRenewalDate(today.plusDays(90))
                .build();

        mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isCreated());

        // Query 30-day window
        mockMvc.perform(get("/api/v1/insurance/renewals/upcoming?windowDays=30")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upcomingCount").value(1))
                .andExpect(jsonPath("$.data.policies[0].policyNumber").value("DUE-SOON-01"));
    }

    @Test
    @DisplayName("Soft-delete policy marks status CANCELLED and dismisses renewal reminder")
    void deletePolicyDismissesReminder() throws Exception {
        LocalDate today = LocalDate.now();
        CreateInsuranceRequest req = CreateInsuranceRequest.builder()
                .policyNumber("DELETE-TEST-01")
                .providerName("Temporary Term")
                .policyType(InsuranceType.LIFE)
                .coverageAmount(new BigDecimal("200000.00"))
                .premiumAmount(new BigDecimal("300.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today)
                .expiryDate(today.plusYears(1))
                .nextRenewalDate(today.plusYears(1))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String policyId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Delete policy
        mockMvc.perform(delete("/api/v1/insurance/" + policyId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Subsequent GET -> 404
        mockMvc.perform(get("/api/v1/insurance/" + policyId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());

        // Verify linked reminder is marked DISMISSED
        Optional<ReminderEntity> reminder = reminderRepository
                .findByTargetEntityIdAndReminderTypeAndIsDeletedFalse(UUID.fromString(policyId), "INSURANCE_RENEWAL");
        assertThat(reminder).isPresent();
        assertThat(reminder.get().getStatus()).isEqualTo(ReminderStatus.DISMISSED);
    }

    @Test
    @DisplayName("Cross-tenant isolation: User B cannot access, update, renew, or delete User A's insurance policy")
    void crossTenantInsuranceIsolation() throws Exception {
        LocalDate today = LocalDate.now();
        CreateInsuranceRequest req = CreateInsuranceRequest.builder()
                .policyNumber("SECRET-HEALTH-99")
                .policyName("Private Executive Medical")
                .providerName("Prestige Health")
                .policyType(InsuranceType.HEALTH)
                .coverageAmount(new BigDecimal("2000000.00"))
                .premiumAmount(new BigDecimal("2500.00"))
                .premiumFrequency(PremiumFrequency.ANNUALLY)
                .startDate(today)
                .expiryDate(today.plusYears(1))
                .nextRenewalDate(today.plusYears(1))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/insurance")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String policyId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. User B tries GET -> 404
        mockMvc.perform(get("/api/v1/insurance/" + policyId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 2. User B tries PUT -> 404
        UpdateInsuranceRequest update = UpdateInsuranceRequest.builder().policyName("Hacked").build();
        mockMvc.perform(put("/api/v1/insurance/" + policyId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        // 3. User B tries RENEW -> 404
        RenewPolicyRequest renew = RenewPolicyRequest.builder().newExpiryDate(today.plusYears(2)).build();
        mockMvc.perform(post("/api/v1/insurance/" + policyId + "/renew")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renew)))
                .andExpect(status().isNotFound());

        // 4. User B tries DELETE -> 404
        mockMvc.perform(delete("/api/v1/insurance/" + policyId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 5. User B upcoming renewals shows 0
        mockMvc.perform(get("/api/v1/insurance/renewals/upcoming")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upcomingCount").value(0));
    }
}
