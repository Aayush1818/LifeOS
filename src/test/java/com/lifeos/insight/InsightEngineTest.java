package com.lifeos.insight;

import com.lifeos.auth.dto.AuthResponse;
import com.lifeos.auth.dto.RegisterRequest;
import com.lifeos.auth.service.AuthService;
import com.lifeos.budget.entity.BudgetEntity;
import com.lifeos.budget.repository.BudgetRepository;
import com.lifeos.finance.entity.PaymentMethod;
import com.lifeos.finance.entity.TransactionCategory;
import com.lifeos.finance.entity.TransactionEntity;
import com.lifeos.finance.entity.TransactionType;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.insight.dto.InsightResponse;
import com.lifeos.insight.dto.InsightSummaryResponse;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.insight.service.InsightService;
import com.lifeos.loan.entity.*;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InsightEngineTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private InsightService insightService;

    private UserEntity userA;
    private UserEntity userB;

    @BeforeEach
    void setUp() {
        RegisterRequest reqA = RegisterRequest.builder()
                .email("insight.userA." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Insight")
                .lastName("Alpha")
                .build();
        AuthResponse resA = authService.register(reqA);
        userA = userRepository.findById(resA.getUser().getId()).orElseThrow();

        RegisterRequest reqB = RegisterRequest.builder()
                .email("insight.userB." + UUID.randomUUID() + "@example.com")
                .password("Password123!@#")
                .firstName("Insight")
                .lastName("Beta")
                .build();
        AuthResponse resB = authService.register(reqB);
        userB = userRepository.findById(resB.getUser().getId()).orElseThrow();
    }

    @Test
    @DisplayName("Detect spending surge, budget depletion, and high-interest debt optimization")
    void testProactiveAnomalyDetection() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // 1. Setup Historical Transactions (trailing 3 months: $100 per month on FOOD_DINING)
        for (int i = 1; i <= 3; i++) {
            TransactionEntity histTx = TransactionEntity.builder()
                    .user(userA)
                    .amount(new BigDecimal("100.00"))
                    .transactionType(TransactionType.EXPENSE)
                    .category(TransactionCategory.FOOD_DINING)
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .transactionDate(startOfMonth.minusMonths(i).plusDays(5))
                    .description("Historical groceries month -" + i)
                    .build();
            transactionRepository.save(histTx);
        }

        // Current month spend on FOOD_DINING: $250 (> 150% of $100 average)
        TransactionEntity surgeTx = TransactionEntity.builder()
                .user(userA)
                .amount(new BigDecimal("250.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD_DINING)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .transactionDate(today)
                .description("Luxury dining & premium groceries")
                .build();
        transactionRepository.save(surgeTx);

        // 2. Setup Budget for ENTERTAINMENT: $200 allocated, $180 spent (90% utilization)
        BudgetEntity budget = BudgetEntity.builder()
                .user(userA)
                .category(TransactionCategory.ENTERTAINMENT)
                .budgetMonth(today.getMonthValue())
                .budgetYear(today.getYear())
                .allocatedAmount(new BigDecimal("200.00"))
                .build();
        budgetRepository.save(budget);

        TransactionEntity budgetTx = TransactionEntity.builder()
                .user(userA)
                .amount(new BigDecimal("180.00"))
                .transactionType(TransactionType.EXPENSE)
                .category(TransactionCategory.ENTERTAINMENT)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .transactionDate(today)
                .description("Concert festival ticket")
                .build();
        transactionRepository.save(budgetTx);

        // 3. Setup High-Interest Active Loan: 12.5% APR
        LoanEntity loan = LoanEntity.builder()
                .user(userA)
                .lenderName("HighRate Finance")
                .loanAccountNumber("LOAN-" + UUID.randomUUID())
                .loanType(LoanType.PERSONAL)
                .principalAmount(new BigDecimal("15000.00"))
                .outstandingBalance(new BigDecimal("12000.00"))
                .interestRate(new BigDecimal("12.50"))
                .interestType(InterestType.FIXED)
                .paymentFrequency(PaymentFrequency.MONTHLY)
                .tenureMonths(36)
                .monthlyEmi(new BigDecimal("501.90"))
                .emiDueDay(15)
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2028, 6, 1))
                .status(LoanStatus.ACTIVE)
                .build();
        loanRepository.save(loan);

        // 4. Generate Insights
        InsightSummaryResponse summary = insightService.generateInsights(userA.getId());

        assertThat(summary.getTotalActive()).isGreaterThanOrEqualTo(3);

        boolean hasSurge = summary.getInsights().stream()
                .anyMatch(i -> i.getInsightType() == InsightType.SPENDING_SURGE && i.getTitle().contains("FOOD_DINING"));
        assertThat(hasSurge).isTrue();

        boolean hasBudgetAlert = summary.getInsights().stream()
                .anyMatch(i -> i.getInsightType() == InsightType.BUDGET_DEPLETION && i.getTitle().contains("ENTERTAINMENT"));
        assertThat(hasBudgetAlert).isTrue();

        boolean hasLoanOptimization = summary.getInsights().stream()
                .anyMatch(i -> i.getInsightType() == InsightType.HIGH_INTEREST_LOAN && i.getTitle().contains("HighRate Finance"));
        assertThat(hasLoanOptimization).isTrue();

        // 5. Test Dismiss and Action state transitions
        InsightResponse firstInsight = summary.getInsights().getFirst();
        InsightResponse dismissed = insightService.dismissInsight(userA.getId(), firstInsight.getId());
        assertThat(dismissed.isDismissed()).isTrue();

        InsightResponse secondInsight = summary.getInsights().get(1);
        InsightResponse actioned = insightService.actionInsight(userA.getId(), secondInsight.getId());
        assertThat(actioned.isActioned()).isTrue();

        // 6. Test Multi-Tenant Isolation
        InsightSummaryResponse userBSummary = insightService.getActiveInsights(userB.getId(), null);
        assertThat(userBSummary.getInsights()).noneMatch(i -> i.getDescription().contains("HighRate Finance"));
    }
}
