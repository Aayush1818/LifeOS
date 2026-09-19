package com.lifeos.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.budget.dto.CreateBudgetRequest;
import com.lifeos.budget.dto.UpdateBudgetRequest;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    private String userAToken;
    private UUID userAId;
    private String userBToken;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("budget.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Budget")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("budget.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Budget")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("Create, update, and soft-delete monthly budget allocation")
    void budgetCrudOperations() throws Exception {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // 1. Create Budget
        CreateBudgetRequest createReq = CreateBudgetRequest.builder()
                .category(TransactionCategory.FOOD_DINING)
                .budgetMonth(month)
                .budgetYear(year)
                .allocatedAmount(new BigDecimal("400.00"))
                .alertThresholds(List.of(50, 75, 90, 100))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("FOOD_DINING"))
                .andExpect(jsonPath("$.data.allocatedAmount").value(400.00))
                .andExpect(jsonPath("$.data.alertThresholds", contains(50, 75, 90, 100)))
                .andReturn();

        String budgetId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 2. Get Budget
        mockMvc.perform(get("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(budgetId))
                .andExpect(jsonPath("$.data.allocatedAmount").value(400.00));

        // 3. Update Budget
        UpdateBudgetRequest updateReq = UpdateBudgetRequest.builder()
                .allocatedAmount(new BigDecimal("500.00"))
                .alertThresholds(List.of(60, 80, 100))
                .build();

        mockMvc.perform(put("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allocatedAmount").value(500.00))
                .andExpect(jsonPath("$.data.alertThresholds", contains(60, 80, 100)));

        // 4. Delete Budget
        mockMvc.perform(delete("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Subsequent GET -> 404
        mockMvc.perform(get("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validation fails on zero or negative budget allocation")
    void createBudgetInvalidAmountFails() throws Exception {
        CreateBudgetRequest invalidReq = CreateBudgetRequest.builder()
                .category(TransactionCategory.ENTERTAINMENT)
                .budgetMonth(5)
                .budgetYear(2026)
                .allocatedAmount(new BigDecimal("-50.00"))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deterministic budget status: spend tracking, remaining, utilization %, and threshold alerts")
    void budgetStatusTrackingAndAlerts() throws Exception {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // 1. Allocate 500.00 budget for FOOD_DINING
        CreateBudgetRequest budgetReq = CreateBudgetRequest.builder()
                .category(TransactionCategory.FOOD_DINING)
                .budgetMonth(month)
                .budgetYear(year)
                .allocatedAmount(new BigDecimal("500.00"))
                .alertThresholds(List.of(50, 75, 90, 100))
                .build();

        mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isCreated());

        // 2. Spend 375.00 (75.00% of budget)
        CreateTransactionRequest tx = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .amount(new BigDecimal("375.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Supermarket haul")
                .build();

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated());

        // 3. Check status: spent=375.00, remaining=125.00, utilization=75.00%, threshold=75, isOverBudget=false
        mockMvc.perform(get("/api/v1/budgets/status?month=" + month + "&year=" + year)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAllocated").value(500.00))
                .andExpect(jsonPath("$.data.totalSpent").value(375.00))
                .andExpect(jsonPath("$.data.totalRemaining").value(125.00))
                .andExpect(jsonPath("$.data.overallUtilizationPercentage").value(75.00))
                .andExpect(jsonPath("$.data.categories[0].category").value("FOOD_DINING"))
                .andExpect(jsonPath("$.data.categories[0].actualSpent").value(375.00))
                .andExpect(jsonPath("$.data.categories[0].remainingAmount").value(125.00))
                .andExpect(jsonPath("$.data.categories[0].utilizationPercentage").value(75.00))
                .andExpect(jsonPath("$.data.categories[0].highestTriggeredThreshold").value(75))
                .andExpect(jsonPath("$.data.categories[0].isOverBudget").value(false));

        // 4. Spend an additional 175.00 (Total spent = 550.00 > 500.00 allocated -> 110.00% utilization)
        CreateTransactionRequest tx2 = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .amount(new BigDecimal("175.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Extra grocery run")
                .build();

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx2)))
                .andExpect(status().isCreated());

        // 5. Check status: over budget
        mockMvc.perform(get("/api/v1/budgets/status?month=" + month + "&year=" + year)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSpent").value(550.00))
                .andExpect(jsonPath("$.data.totalRemaining").value(-50.00))
                .andExpect(jsonPath("$.data.overallUtilizationPercentage").value(110.00))
                .andExpect(jsonPath("$.data.categories[0].isOverBudget").value(true))
                .andExpect(jsonPath("$.data.categories[0].highestTriggeredThreshold").value(100));
    }

    @Test
    @DisplayName("Cross-tenant isolation: User B cannot access, update, or delete User A's budget")
    void crossTenantBudgetIsolation() throws Exception {
        LocalDate today = LocalDate.now();

        CreateBudgetRequest budgetReq = CreateBudgetRequest.builder()
                .category(TransactionCategory.HEALTHCARE)
                .budgetMonth(today.getMonthValue())
                .budgetYear(today.getYear())
                .allocatedAmount(new BigDecimal("300.00"))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/budgets")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String budgetId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. User B tries GET -> 404
        mockMvc.perform(get("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 2. User B tries PUT -> 404
        UpdateBudgetRequest updateReq = UpdateBudgetRequest.builder()
                .allocatedAmount(new BigDecimal("999.00"))
                .build();
        mockMvc.perform(put("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // 3. User B tries DELETE -> 404
        mockMvc.perform(delete("/api/v1/budgets/" + budgetId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 4. User B status endpoint shows 0 allocated and 0 spent
        mockMvc.perform(get("/api/v1/budgets/status?month=" + today.getMonthValue() + "&year=" + today.getYear())
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAllocated").value(0))
                .andExpect(jsonPath("$.data.totalSpent").value(0))
                .andExpect(jsonPath("$.data.categories", hasSize(0)));
    }
}
