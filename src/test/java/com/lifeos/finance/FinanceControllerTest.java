package com.lifeos.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.finance.dto.CreateRecurringRequest;
import com.lifeos.finance.dto.CreateTransactionRequest;
import com.lifeos.finance.dto.UpdateRecurringRequest;
import com.lifeos.finance.dto.UpdateTransactionRequest;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.RecurrencePattern;
import com.lifeos.finance.entity.RecurringStatus;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceControllerTest {

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
                .email("finance.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Finance")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("finance.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Finance")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("Record income and expense transactions with BigDecimal precision")
    void recordTransactionsSuccess() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Create Income
        CreateTransactionRequest incomeReq = CreateTransactionRequest.builder()
                .transactionType(TransactionType.INCOME)
                .category(TransactionCategory.INCOME_SALARY)
                .amount(new BigDecimal("5000.50"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionDate(today)
                .description("Monthly engineering salary")
                .build();

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(5000.50))
                .andExpect(jsonPath("$.data.transactionType").value("INCOME"))
                .andExpect(jsonPath("$.data.category").value("INCOME_SALARY"))
                .andExpect(jsonPath("$.data.description").value("Monthly engineering salary"));

        // 2. Create Expense
        CreateTransactionRequest expenseReq = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .amount(new BigDecimal("150.75"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Weekly supermarket groceries")
                .build();

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(150.75))
                .andExpect(jsonPath("$.data.transactionType").value("EXPENSE"))
                .andExpect(jsonPath("$.data.category").value("FOOD_DINING"));
    }

    @Test
    @DisplayName("Validation fails when transaction amount is zero or negative")
    void recordTransactionInvalidAmountFails() throws Exception {
        CreateTransactionRequest invalidReq = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .amount(new BigDecimal("-10.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(LocalDate.now())
                .description("Invalid negative amount")
                .build();

        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Duplicate transaction detection flags potential duplicate warning")
    void duplicateTransactionWarningFlagged() throws Exception {
        LocalDate today = LocalDate.now();

        CreateTransactionRequest req1 = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.ENTERTAINMENT)
                .amount(new BigDecimal("49.99"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Concert ticket")
                .build();

        // First creation -> no duplicate warning
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.possibleDuplicateWarning").value(false));

        // Duplicate submission -> duplicate warning true
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.possibleDuplicateWarning").value(true));
    }

    @Test
    @DisplayName("Update transaction details and soft-delete")
    void updateAndDeleteTransaction() throws Exception {
        LocalDate today = LocalDate.now();

        CreateTransactionRequest createReq = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.UTILITIES)
                .amount(new BigDecimal("120.00"))
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .transactionDate(today)
                .description("Water bill")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String txId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // Update transaction
        UpdateTransactionRequest updateReq = UpdateTransactionRequest.builder()
                .amount(new BigDecimal("135.50"))
                .description("Water and sewage bill updated")
                .build();

        mockMvc.perform(put("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(135.50))
                .andExpect(jsonPath("$.data.description").value("Water and sewage bill updated"));

        // Delete transaction (soft delete)
        mockMvc.perform(delete("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Subsequent GET -> 404
        mockMvc.perform(get("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Refund modeling correctly deducts from expenses in financial analytics")
    void refundModelingInAnalytics() throws Exception {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // Income: 3000.00
        CreateTransactionRequest salary = CreateTransactionRequest.builder()
                .transactionType(TransactionType.INCOME)
                .category(TransactionCategory.INCOME_SALARY)
                .amount(new BigDecimal("3000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .transactionDate(today)
                .description("Salary")
                .build();
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isCreated());

        // Expense: 200.00
        CreateTransactionRequest purchase = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.SHOPPING)
                .amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Clothing purchase")
                .build();
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purchase)))
                .andExpect(status().isCreated());

        // Refund: 50.00 (expense refund)
        CreateTransactionRequest refund = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.SHOPPING)
                .amount(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Returned shirt refund")
                .isRefund(true)
                .build();
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refund)))
                .andExpect(status().isCreated());

        // Query Monthly Summary: Total Expenses should be 200.00 - 50.00 = 150.00
        // Net Savings = 3000.00 - 150.00 = 2850.00
        mockMvc.perform(get("/api/v1/finance/analytics/monthly-summary?month=" + month + "&year=" + year)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalIncome").value(3000.00))
                .andExpect(jsonPath("$.data.totalExpenses").value(150.00))
                .andExpect(jsonPath("$.data.netSavings").value(2850.00))
                .andExpect(jsonPath("$.data.savingsRatePercentage").value(95.00));
    }

    @Test
    @DisplayName("Category breakdown and Month-over-Month comparison analytics")
    void categoryBreakdownAndMoMAnalytics() throws Exception {
        LocalDate currentMonthDate = LocalDate.now();
        int curMonth = currentMonthDate.getMonthValue();
        int curYear = currentMonthDate.getYear();

        // 1. Current month expenses: Food 200.00, Utilities 100.00 (Total = 300.00)
        CreateTransactionRequest g = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(currentMonthDate)
                .description("Supermarket")
                .build();
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(g)))
                .andExpect(status().isCreated());

        CreateTransactionRequest d = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.UTILITIES)
                .amount(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(currentMonthDate)
                .description("Electricity")
                .build();
        mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(d)))
                .andExpect(status().isCreated());

        // Category breakdown check
        mockMvc.perform(get("/api/v1/finance/analytics/category-breakdown?month=" + curMonth + "&year=" + curYear)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));

        // MoM check
        mockMvc.perform(get("/api/v1/finance/analytics/month-over-month?month=" + curMonth + "&year=" + curYear)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentMonthExpenses").value(300.00))
                .andExpect(jsonPath("$.data.direction").value("INCREASED"));
    }

    @Test
    @DisplayName("Recurring transaction CRUD and subscription management")
    void recurringTransactionCrud() throws Exception {
        CreateRecurringRequest req = CreateRecurringRequest.builder()
                .title("Cloud Infrastructure")
                .amount(new BigDecimal("45.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.UTILITIES)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .recurrencePattern(RecurrencePattern.MONTHLY)
                .billingDay(15)
                .startDate(LocalDate.of(2026, 1, 1))
                .autoCreateTransaction(true)
                .notes("Monthly cloud hosting")
                .build();

        // 1. Create recurring rule
        MvcResult res = mockMvc.perform(post("/api/v1/finance/recurring")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Cloud Infrastructure"))
                .andExpect(jsonPath("$.data.amount").value(45.00))
                .andExpect(jsonPath("$.data.recurrencePattern").value("MONTHLY"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        String recurringId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // 2. List recurring rules
        mockMvc.perform(get("/api/v1/finance/recurring")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Cloud Infrastructure"));

        // 3. Update recurring rule
        UpdateRecurringRequest updateReq = UpdateRecurringRequest.builder()
                .amount(new BigDecimal("55.00"))
                .status(RecurringStatus.PAUSED)
                .build();

        mockMvc.perform(put("/api/v1/finance/recurring/" + recurringId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(55.00))
                .andExpect(jsonPath("$.data.status").value("PAUSED"));

        // 4. Delete recurring rule
        mockMvc.perform(delete("/api/v1/finance/recurring/" + recurringId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk());

        // Subsequent GET -> 404
        mockMvc.perform(get("/api/v1/finance/recurring/" + recurringId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Cross-tenant isolation: User B cannot view, update, or delete User A's transactions or recurring rules")
    void crossTenantIsolation() throws Exception {
        // User A creates transaction
        CreateTransactionRequest txReq = CreateTransactionRequest.builder()
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.OTHER)
                .amount(new BigDecimal("99.99"))
                .paymentMethod(PaymentMethod.CASH)
                .transactionDate(LocalDate.now())
                .description("User A secret transaction")
                .build();

        MvcResult txRes = mockMvc.perform(post("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String txId = objectMapper.readTree(txRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // User A creates recurring rule
        CreateRecurringRequest recReq = CreateRecurringRequest.builder()
                .title("User A secret subscription")
                .amount(new BigDecimal("12.99"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.ENTERTAINMENT)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .recurrencePattern(RecurrencePattern.MONTHLY)
                .billingDay(1)
                .startDate(LocalDate.now())
                .build();

        MvcResult recRes = mockMvc.perform(post("/api/v1/finance/recurring")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String recId = objectMapper.readTree(recRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. User B tries GET transaction -> 404
        mockMvc.perform(get("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 2. User B tries PUT transaction -> 404
        UpdateTransactionRequest updateTx = UpdateTransactionRequest.builder().description("Hacked").build();
        mockMvc.perform(put("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTx)))
                .andExpect(status().isNotFound());

        // 3. User B tries DELETE transaction -> 404
        mockMvc.perform(delete("/api/v1/finance/transactions/" + txId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 4. User B tries GET recurring -> 404
        mockMvc.perform(get("/api/v1/finance/recurring/" + recId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 5. User B tries PUT recurring -> 404
        UpdateRecurringRequest updateRec = UpdateRecurringRequest.builder().title("Hacked").build();
        mockMvc.perform(put("/api/v1/finance/recurring/" + recId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRec)))
                .andExpect(status().isNotFound());

        // 6. User B tries DELETE recurring -> 404
        mockMvc.perform(delete("/api/v1/finance/recurring/" + recId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 7. User B transaction list is isolated (0 results)
        mockMvc.perform(get("/api/v1/finance/transactions")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
