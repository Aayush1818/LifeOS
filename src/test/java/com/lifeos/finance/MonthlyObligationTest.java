package com.lifeos.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.RecurrencePattern;
import com.lifeos.finance.entity.RecurringStatus;
import com.lifeos.finance.entity.RecurringTransactionEntity;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import com.lifeos.insurance.entity.InsurancePolicyEntity;
import com.lifeos.insurance.entity.InsuranceType;
import com.lifeos.insurance.entity.PolicyStatus;
import com.lifeos.insurance.entity.PremiumFrequency;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.loan.entity.InterestType;
import com.lifeos.loan.entity.LoanEntity;
import com.lifeos.loan.entity.LoanStatus;
import com.lifeos.loan.entity.LoanType;
import com.lifeos.loan.entity.PaymentFrequency;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.travel.entity.TripEntity;
import com.lifeos.travel.entity.TripStatus;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MonthlyObligationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;

    @Autowired
    private TripRepository tripRepository;

    private String userAToken;
    private UserEntity userA;
    private String userBToken;
    private UserEntity userB;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("obligation.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Obligation")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userAToken = resA.getAccessToken();
        userA = userRepository.findById(resA.getUser().getId()).orElseThrow();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("obligation.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Obligation")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userBToken = resB.getAccessToken();
        userB = userRepository.findById(resB.getUser().getId()).orElseThrow();
    }

    @Test
    @DisplayName("Synthesize monthly obligations across loans, insurance, recurring bills, and trips with net cash flow")
    void getMonthlyObligationsSuccess() throws Exception {
        int targetMonth = 10;
        int targetYear = 2026;

        // 1. Create active Loan EMI for User A: $1,250.00 due on 5th
        LoanEntity loan = LoanEntity.builder()
                .user(userA)
                .lenderName("Chase Bank")
                .loanAccountNumber("LOAN-" + UUID.randomUUID())
                .loanType(LoanType.HOME)
                .principalAmount(new BigDecimal("350000.00"))
                .outstandingBalance(new BigDecimal("320000.00"))
                .interestRate(new BigDecimal("5.75"))
                .interestType(InterestType.FIXED)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .tenureMonths(360)
                .monthlyEmi(new BigDecimal("1250.00"))
                .emiDueDay(5)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2055, 1, 1))
                .status(LoanStatus.ACTIVE)
                .build();
        loanRepository.save(loan);

        // 2. Create active Insurance Policy for User A: $150.00 due on 12th
        InsurancePolicyEntity policy = InsurancePolicyEntity.builder()
                .user(userA)
                .policyNumber("POL-" + UUID.randomUUID())
                .policyName("Comprehensive Auto Coverage")
                .providerName("Geico")
                .policyType(InsuranceType.VEHICLE)
                .coverageAmount(new BigDecimal("100000.00"))
                .premiumAmount(new BigDecimal("150.00"))
                .premiumFrequency(PremiumFrequency.MONTHLY)
                .startDate(LocalDate.of(2026, 1, 1))
                .expiryDate(LocalDate.of(2027, 1, 1))
                .nextRenewalDate(LocalDate.of(2026, 10, 12))
                .status(PolicyStatus.ACTIVE)
                .build();
        insurancePolicyRepository.save(policy);

        // 3. Create active Recurring Bill (expense) for User A: $80.00 due on 18th
        RecurringTransactionEntity bill = RecurringTransactionEntity.builder()
                .user(userA)
                .title("High Speed Fiber Internet")
                .amount(new BigDecimal("80.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.UTILITIES)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .recurrencePattern(RecurrencePattern.MONTHLY)
                .billingDay(18)
                .startDate(LocalDate.of(2026, 1, 1))
                .nextDueDate(LocalDate.of(2026, 10, 18))
                .status(RecurringStatus.ACTIVE)
                .build();
        recurringTransactionRepository.save(bill);

        // 4. Create active Recurring Income for User A: $5,000.00
        RecurringTransactionEntity salary = RecurringTransactionEntity.builder()
                .user(userA)
                .title("Monthly Software Engineering Salary")
                .amount(new BigDecimal("5000.00"))
                .transactionType(TransactionType.INCOME)
                .category(TransactionCategory.INCOME_SALARY)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .recurrencePattern(RecurrencePattern.MONTHLY)
                .billingDay(1)
                .startDate(LocalDate.of(2026, 1, 1))
                .nextDueDate(LocalDate.of(2026, 10, 1))
                .status(RecurringStatus.ACTIVE)
                .build();
        recurringTransactionRepository.save(salary);

        // 5. Create planned Trip for User A: $2,000 budget - $500 spend = $1,500 remaining, starts on Oct 20
        TripEntity trip = TripEntity.builder()
                .user(userA)
                .tripTitle("Tokyo Tech Summit")
                .destination("Tokyo, Japan")
                .startDate(LocalDate.of(2026, 10, 20))
                .endDate(LocalDate.of(2026, 10, 26))
                .totalBudget(new BigDecimal("2000.00"))
                .actualSpend(new BigDecimal("500.00"))
                .currency("USD")
                .status(TripStatus.PLANNED)
                .build();
        tripRepository.save(trip);

        // Query User A obligations for October 2026
        // Total: 1250.00 + 150.00 + 80.00 + 1500.00 = 2980.00
        // Net Surplus: 5000.00 - 2980.00 = 2020.00
        mockMvc.perform(get("/api/v1/finance/obligations/monthly?month=" + targetMonth + "&year=" + targetYear)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.month").value(10))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.loanEmisTotal").value(1250.00))
                .andExpect(jsonPath("$.data.insurancePremiumsTotal").value(150.00))
                .andExpect(jsonPath("$.data.recurringBillsTotal").value(80.00))
                .andExpect(jsonPath("$.data.tripAllocationsTotal").value(1500.00))
                .andExpect(jsonPath("$.data.totalObligationAmount").value(2980.00))
                .andExpect(jsonPath("$.data.projectedIncome").value(5000.00))
                .andExpect(jsonPath("$.data.netSurplusOrDeficit").value(2020.00))
                .andExpect(jsonPath("$.data.items", hasSize(4)))
                .andExpect(jsonPath("$.data.items[0].category").value("LOAN_EMI"))
                .andExpect(jsonPath("$.data.items[0].amount").value(1250.00))
                .andExpect(jsonPath("$.data.items[1].category").value("INSURANCE_PREMIUM"))
                .andExpect(jsonPath("$.data.items[1].amount").value(150.00))
                .andExpect(jsonPath("$.data.items[2].category").value("UTILITIES"))
                .andExpect(jsonPath("$.data.items[2].amount").value(80.00))
                .andExpect(jsonPath("$.data.items[3].category").value("TRAVEL"))
                .andExpect(jsonPath("$.data.items[3].amount").value(1500.00));
    }

    @Test
    @DisplayName("Cross-tenant isolation: User B has empty obligations and zero totals")
    void crossTenantObligationsIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/finance/obligations/monthly?month=10&year=2026")
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalObligationAmount").value(0.0))
                .andExpect(jsonPath("$.data.items", hasSize(0)));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/finance/obligations/monthly?month=10&year=2026"))
                .andExpect(status().isUnauthorized());
    }
}
