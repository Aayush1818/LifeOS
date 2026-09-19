package com.lifeos.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.loan.dto.CreateLoanRequest;
import com.lifeos.loan.dto.RecordLoanPaymentRequest;
import com.lifeos.loan.dto.UpdateLoanRequest;
import com.lifeos.loan.entity.InterestType;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.entity.LoanType;
import com.lifeos.loan.entity.PaymentFrequency;
import com.lifeos.loan.entity.PaymentType;
import com.lifeos.loan.entity.PrepaymentStrategy;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerTest {

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
                .email("loan.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("LoanUser")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userAId = resA.getUser().getId();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("loan.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("LoanUser")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userBId = resB.getUser().getId();
    }

    @Test
    @DisplayName("Create loan records details and auto-calculates monthly EMI with BigDecimal precision")
    void createLoanSuccess() throws Exception {
        CreateLoanRequest request = CreateLoanRequest.builder()
                .loanAccountNumber("HL-2026-9901")
                .lenderName("Apex Mortgage Bank")
                .loanType(LoanType.HOME)
                .principalAmount(new BigDecimal("250000.00"))
                .interestRate(new BigDecimal("6.50"))
                .interestType(InterestType.FIXED)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .tenureMonths(240) // 20 years
                .emiDueDay(5)
                .startDate(LocalDate.of(2026, 1, 1))
                .notes("Primary residence home loan")
                .build();

        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loanAccountNumber").value("HL-2026-9901"))
                .andExpect(jsonPath("$.data.principalAmount").value(250000.00))
                .andExpect(jsonPath("$.data.outstandingBalance").value(250000.00))
                .andExpect(jsonPath("$.data.interestRate").value(6.50))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.monthlyEmi", greaterThan(1800.00)));
    }

    @Test
    @DisplayName("Duplicate loan account number is rejected with 409 Conflict")
    void duplicateLoanAccountNumberRejected() throws Exception {
        CreateLoanRequest request = CreateLoanRequest.builder()
                .loanAccountNumber("AUTO-DUPLICATE-CHECK")
                .lenderName("Auto Credit")
                .loanType(LoanType.VEHICLE)
                .principalAmount(new BigDecimal("35000.00"))
                .interestRate(new BigDecimal("8.00"))
                .tenureMonths(60)
                .emiDueDay(10)
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate submission
        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Generate amortization schedule validates all mathematical invariants")
    void getAmortizationScheduleInvariants() throws Exception {
        CreateLoanRequest request = CreateLoanRequest.builder()
                .loanAccountNumber("SCHED-TEST-01")
                .lenderName("National Finance")
                .loanType(LoanType.PERSONAL)
                .principalAmount(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("9.50"))
                .tenureMonths(48)
                .emiDueDay(1)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String loanId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asText();

        MvcResult scheduleRes = mockMvc.perform(get("/api/v1/loans/" + loanId + "/schedule")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.installments", hasSize(48)))
                .andReturn();

        var root = objectMapper.readTree(scheduleRes.getResponse().getContentAsString()).get("data");
        BigDecimal principal = new BigDecimal(root.get("principal").asText());
        BigDecimal totalInterest = new BigDecimal(root.get("totalInterest").asText());
        BigDecimal totalPayment = new BigDecimal(root.get("totalPayment").asText());

        var installments = root.get("installments");
        BigDecimal sumPrincipal = BigDecimal.ZERO;
        BigDecimal sumPayments = BigDecimal.ZERO;

        for (var inst : installments) {
            sumPrincipal = sumPrincipal.add(new BigDecimal(inst.get("principalComponent").asText()));
            sumPayments = sumPayments.add(new BigDecimal(inst.get("payment").asText()));
        }

        // Invariant 1: Sum of principal components == original principal
        assertThat(sumPrincipal).isEqualByComparingTo(principal);

        // Invariant 2: Final installment closing principal == 0.00
        BigDecimal lastClosing = new BigDecimal(installments.get(47).get("closingPrincipal").asText());
        assertThat(lastClosing).isEqualByComparingTo("0.00");

        // Invariant 3: Total payments == principal + total interest
        assertThat(totalPayment).isEqualByComparingTo(principal.add(totalInterest));
        assertThat(sumPayments).isEqualByComparingTo(totalPayment);
    }

    @Test
    @DisplayName("Record regular EMI, partial prepayment, and full early closure")
    void recordPaymentsAndPrepayment() throws Exception {
        CreateLoanRequest createReq = CreateLoanRequest.builder()
                .loanAccountNumber("PAY-PREPAY-01")
                .lenderName("City Credit Union")
                .loanType(LoanType.PERSONAL)
                .principalAmount(new BigDecimal("20000.00"))
                .interestRate(new BigDecimal("10.00"))
                .tenureMonths(24)
                .emiDueDay(15)
                .startDate(LocalDate.of(2026, 1, 1))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String loanId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. Record regular EMI payment
        RecordLoanPaymentRequest emiPayment = RecordLoanPaymentRequest.builder()
                .paymentAmount(new BigDecimal("922.90"))
                .paymentDate(LocalDate.of(2026, 2, 15))
                .paymentType(PaymentType.REGULAR_EMI)
                .transactionRef("TXN-EMI-001")
                .build();

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emiPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentType").value("REGULAR_EMI"))
                .andExpect(jsonPath("$.data.interestComponent", greaterThan(0.00)));

        // 2. Record partial prepayment ($5,000.00) under REDUCE_EMI strategy
        RecordLoanPaymentRequest prepayment = RecordLoanPaymentRequest.builder()
                .paymentAmount(new BigDecimal("5000.00"))
                .paymentDate(LocalDate.of(2026, 3, 1))
                .paymentType(PaymentType.PARTIAL_PREPAYMENT)
                .prepaymentStrategy(PrepaymentStrategy.REDUCE_EMI)
                .transactionRef("TXN-PREPAY-001")
                .build();

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.principalComponent").value(5000.00))
                .andExpect(jsonPath("$.data.interestComponent").value(0.00));

        // Verify loan balance and recomputed EMI
        MvcResult loanDetailRes = mockMvc.perform(get("/api/v1/loans/" + loanId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal balanceAfterPrepay = new BigDecimal(objectMapper.readTree(loanDetailRes.getResponse().getContentAsString())
                .get("data").get("outstandingBalance").asText());
        BigDecimal newEmi = new BigDecimal(objectMapper.readTree(loanDetailRes.getResponse().getContentAsString())
                .get("data").get("monthlyEmi").asText());

        assertThat(balanceAfterPrepay).isLessThan(new BigDecimal("15000.00"));
        assertThat(newEmi).isLessThan(new BigDecimal("922.90"));

        // 3. Full early closure: pay exact remaining balance
        RecordLoanPaymentRequest fullClosure = RecordLoanPaymentRequest.builder()
                .paymentAmount(balanceAfterPrepay)
                .paymentDate(LocalDate.of(2026, 4, 1))
                .paymentType(PaymentType.FULL_CLOSURE)
                .transactionRef("TXN-CLOSE-FINAL")
                .build();

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullClosure)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentType").value("FULL_CLOSURE"));

        // Verify loan status is now CLOSED and balance is 0.00
        mockMvc.perform(get("/api/v1/loans/" + loanId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.outstandingBalance").value(0.00));
    }

    @Test
    @DisplayName("Loan portfolio summary calculates totals via direct JDBC pushdown")
    void loanPortfolioSummary() throws Exception {
        CreateLoanRequest loan1 = CreateLoanRequest.builder()
                .loanAccountNumber("PORT-01")
                .lenderName("Bank One")
                .loanType(LoanType.EDUCATION)
                .principalAmount(new BigDecimal("40000.00"))
                .interestRate(new BigDecimal("5.00"))
                .tenureMonths(60)
                .emiDueDay(1)
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loan1)))
                .andExpect(status().isCreated());

        CreateLoanRequest loan2 = CreateLoanRequest.builder()
                .loanAccountNumber("PORT-02")
                .lenderName("Bank Two")
                .loanType(LoanType.VEHICLE)
                .principalAmount(new BigDecimal("25000.00"))
                .interestRate(new BigDecimal("7.00"))
                .tenureMonths(36)
                .emiDueDay(1)
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loan2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/loans/analytics/summary")
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeLoansCount").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalOriginalPrincipal").value(greaterThanOrEqualTo(65000.00)))
                .andExpect(jsonPath("$.data.totalOutstandingBalance").value(greaterThanOrEqualTo(65000.00)));
    }

    @Test
    @DisplayName("Cross-tenant isolation: User B cannot view, pay, schedule, or delete User A's loan")
    void crossTenantLoanIsolation() throws Exception {
        CreateLoanRequest loan = CreateLoanRequest.builder()
                .loanAccountNumber("ISOLATION-01")
                .lenderName("Secret Bank")
                .loanType(LoanType.PERSONAL)
                .principalAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("10.00"))
                .tenureMonths(12)
                .emiDueDay(1)
                .startDate(LocalDate.now())
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/loans")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loan)))
                .andExpect(status().isCreated())
                .andReturn();

        String loanId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asText();

        // 1. User B tries GET loan -> 404
        mockMvc.perform(get("/api/v1/loans/" + loanId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 2. User B tries GET schedule -> 404
        mockMvc.perform(get("/api/v1/loans/" + loanId + "/schedule")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 3. User B tries POST payment -> 404
        RecordLoanPaymentRequest pay = RecordLoanPaymentRequest.builder()
                .paymentAmount(new BigDecimal("100.00"))
                .paymentDate(LocalDate.now())
                .build();
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay)))
                .andExpect(status().isNotFound());

        // 4. User B tries PUT loan -> 404
        UpdateLoanRequest update = UpdateLoanRequest.builder().lenderName("Hacked").build();
        mockMvc.perform(put("/api/v1/loans/" + loanId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        // 5. User B tries DELETE loan -> 404
        mockMvc.perform(delete("/api/v1/loans/" + loanId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // 6. User B summary shows 0 active loans
        mockMvc.perform(get("/api/v1/loans/analytics/summary")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeLoansCount").value(0))
                .andExpect(jsonPath("$.data.totalOutstandingBalance").value(0));
    }
}
