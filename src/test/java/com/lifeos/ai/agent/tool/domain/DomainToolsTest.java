package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.finance.dto.MonthlySummaryResponse;
import com.lifeos.finance.service.FinanceAnalyticsService;
import com.lifeos.healthcare.dto.AppointmentResponse;
import com.lifeos.healthcare.service.AppointmentService;
import com.lifeos.insurance.dto.InsuranceResponse;
import com.lifeos.insurance.service.InsuranceService;
import com.lifeos.loan.dto.LoanPaymentResponse;
import com.lifeos.loan.dto.LoanPortfolioSummaryResponse;
import com.lifeos.loan.service.LoanAnalyticsService;
import com.lifeos.loan.service.LoanService;
import com.lifeos.rag.retrieval.dto.RetrievalResponse;
import com.lifeos.rag.retrieval.service.RetrievalService;
import com.lifeos.reminder.entity.ReminderEntity;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.travel.dto.TripDetailResponse;
import com.lifeos.travel.service.TripService;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainToolsTest {

    @Mock
    private LoanAnalyticsService loanAnalyticsService;
    @Mock
    private LoanService loanService;
    @Mock
    private InsuranceService insuranceService;
    @Mock
    private FinanceAnalyticsService financeAnalyticsService;
    @Mock
    private com.lifeos.finance.obligation.service.FinancialObligationService financialObligationService;
    @Mock
    private com.lifeos.insight.service.InsightService insightService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private TripService tripService;
    @Mock
    private RetrievalService retrievalService;
    @Mock
    private ReminderRepository reminderRepository;
    @Mock
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("LoanSummaryTool should query analytics service and return summary")
    void testLoanSummaryTool() {
        LoanSummaryTool tool = new LoanSummaryTool(loanAnalyticsService);
        assertThat(tool.getName()).isEqualTo("get_loan_summary");
        assertThat(tool.requiresConfirmation()).isFalse();

        LoanPortfolioSummaryResponse summary = LoanPortfolioSummaryResponse.builder()
                .activeLoansCount(2)
                .totalOutstandingBalance(BigDecimal.valueOf(50000))
                .totalMonthlyEmi(BigDecimal.valueOf(1200))
                .build();
        when(loanAnalyticsService.getPortfolioSummary(userId)).thenReturn(summary);

        ToolExecutionResult result = tool.execute(userId, Map.of());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(summary);
    }

    @Test
    @DisplayName("InsuranceRenewalsTool should return upcoming policies")
    void testInsuranceRenewalsTool() {
        InsuranceRenewalsTool tool = new InsuranceRenewalsTool(insuranceService);
        assertThat(tool.getName()).isEqualTo("get_insurance_policies");
        assertThat(tool.requiresConfirmation()).isFalse();

        InsuranceResponse policy = InsuranceResponse.builder()
                .policyName("Health Comprehensive")
                .policyNumber("POL-1001")
                .nextRenewalDate(LocalDate.now().plusDays(20))
                .build();
        when(insuranceService.listPolicies(eq(userId), isNull(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(policy)));

        ToolExecutionResult result = tool.execute(userId, Map.of("windowDays", 60));
        assertThat(result.isSuccess()).isTrue();
        assertThat((List<?>) result.getOutput()).hasSize(1);
    }

    @Test
    @DisplayName("MonthlySpendSummaryTool should invoke finance analytics service")
    void testMonthlySpendSummaryTool() {
        MonthlySpendSummaryTool tool = new MonthlySpendSummaryTool(financeAnalyticsService);
        assertThat(tool.getName()).isEqualTo("get_monthly_spend_summary");
        assertThat(tool.requiresConfirmation()).isFalse();

        MonthlySummaryResponse summary = MonthlySummaryResponse.builder()
                .year(2026)
                .month(9)
                .totalExpenses(BigDecimal.valueOf(3200))
                .build();
        when(financeAnalyticsService.getMonthlySummary(eq(userId), eq(9), eq(2026))).thenReturn(summary);

        ToolExecutionResult result = tool.execute(userId, Map.of("year", 2026, "month", 9));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(summary);
    }

    @Test
    @DisplayName("MonthlyObligationsTool should invoke financial obligation service")
    void testMonthlyObligationsTool() {
        MonthlyObligationsTool tool = new MonthlyObligationsTool(financialObligationService);
        assertThat(tool.getName()).isEqualTo("get_monthly_obligations");
        assertThat(tool.requiresConfirmation()).isFalse();

        com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse response =
                com.lifeos.finance.obligation.dto.MonthlyObligationSummaryResponse.builder()
                        .year(2026)
                        .month(10)
                        .totalObligationAmount(new BigDecimal("2980.00"))
                        .projectedIncome(new BigDecimal("5000.00"))
                        .netSurplusOrDeficit(new BigDecimal("2020.00"))
                        .build();

        when(financialObligationService.getMonthlyObligations(eq(userId), eq(10), eq(2026))).thenReturn(response);

        ToolExecutionResult result = tool.execute(userId, Map.of("year", 2026, "month", 10));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(response);
    }

    @Test
    @DisplayName("ProactiveInsightsTool should invoke insight service and return summary")
    void testProactiveInsightsTool() {
        ProactiveInsightsTool tool = new ProactiveInsightsTool(insightService);
        assertThat(tool.getName()).isEqualTo("get_proactive_insights");
        assertThat(tool.requiresConfirmation()).isFalse();

        com.lifeos.insight.dto.InsightSummaryResponse summary =
                com.lifeos.insight.dto.InsightSummaryResponse.builder()
                        .totalActive(2)
                        .warningCount(1)
                        .criticalCount(1)
                        .build();

        when(insightService.getActiveInsights(eq(userId), eq(com.lifeos.insight.entity.InsightSeverity.WARNING))).thenReturn(summary);

        ToolExecutionResult result = tool.execute(userId, Map.of("minSeverity", "WARNING"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(summary);
    }

    @Test
    @DisplayName("HealthcareAppointmentsTool should return upcoming appointments")
    void testHealthcareAppointmentsTool() {
        HealthcareAppointmentsTool tool = new HealthcareAppointmentsTool(appointmentService);
        assertThat(tool.getName()).isEqualTo("get_upcoming_appointments");
        assertThat(tool.requiresConfirmation()).isFalse();

        AppointmentResponse appt = AppointmentResponse.builder()
                .doctorName("Dr. Strange")
                .clinicOrHospital("Metro Health")
                .build();
        when(appointmentService.listAppointments(eq(userId), isNull(), any(), any(), any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(appt)));

        ToolExecutionResult result = tool.execute(userId, Map.of("windowDays", 30));
        assertThat(result.isSuccess()).isTrue();
        assertThat((List<?>) result.getOutput()).hasSize(1);
    }

    @Test
    @DisplayName("TripItineraryTool should return itinerary items when tripId is valid")
    void testTripItineraryTool() {
        TripItineraryTool tool = new TripItineraryTool(tripService);
        assertThat(tool.getName()).isEqualTo("get_trip_itinerary");
        assertThat(tool.requiresConfirmation()).isFalse();

        UUID tripId = UUID.randomUUID();
        TripDetailResponse detail = TripDetailResponse.builder()
                .id(tripId)
                .tripTitle("Tokyo Autumn")
                .destination("Tokyo, Japan")
                .build();
        when(tripService.getTrip(eq(tripId), eq(userId))).thenReturn(detail);

        ToolExecutionResult result = tool.execute(userId, Map.of("tripId", tripId.toString()));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(detail);
    }

    @Test
    @DisplayName("DocumentSearchTool should return search results for non-empty query")
    void testDocumentSearchTool() {
        DocumentSearchTool tool = new DocumentSearchTool(retrievalService);
        assertThat(tool.getName()).isEqualTo("search_documents");
        assertThat(tool.requiresConfirmation()).isFalse();

        RetrievalResponse retrievalResponse = RetrievalResponse.builder()
                .hasRelevantContext(true)
                .results(List.of())
                .build();
        when(retrievalService.retrieve(eq(userId), any())).thenReturn(retrievalResponse);

        ToolExecutionResult result = tool.execute(userId, Map.of("query", "insurance deductible"));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(retrievalResponse.getResults());

        // Missing query
        ToolExecutionResult failure = tool.execute(userId, Map.of());
        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getErrorMessage()).contains("query");
    }

    @Test
    @DisplayName("CreateReminderTool requires confirmation and creates reminder when confirmed")
    void testCreateReminderTool() {
        CreateReminderTool tool = new CreateReminderTool(reminderRepository, userRepository);
        assertThat(tool.getName()).isEqualTo("create_reminder");
        assertThat(tool.requiresConfirmation()).isTrue();

        String prompt = tool.getConfirmationPrompt(Map.of("title", "Dental checkup", "dueAt", "2026-10-01T10:00:00Z"));
        assertThat(prompt).contains("Dental checkup");

        UserEntity user = UserEntity.builder().email("u@test.com").firstName("A").lastName("B").build();
        user.setId(userId);
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
        when(reminderRepository.save(any())).thenAnswer(inv -> {
            ReminderEntity r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ToolExecutionResult result = tool.execute(userId, Map.of(
                "title", "Dental checkup",
                "dueAt", "2026-10-01T10:00:00Z",
                "description", "Bring insurance card"
        ));

        assertThat(result.isSuccess()).isTrue();
        verify(reminderRepository).save(any(ReminderEntity.class));

        // Missing parameters validation
        ToolExecutionResult missingTitle = tool.execute(userId, Map.of("dueAt", "2026-10-01T10:00:00Z"));
        assertThat(missingTitle.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("RecordLoanPaymentTool requires confirmation and records payment when confirmed")
    void testRecordLoanPaymentTool() {
        RecordLoanPaymentTool tool = new RecordLoanPaymentTool(loanService);
        assertThat(tool.getName()).isEqualTo("record_loan_payment");
        assertThat(tool.requiresConfirmation()).isTrue();

        UUID loanId = UUID.randomUUID();
        String prompt = tool.getConfirmationPrompt(Map.of("amount", "750", "loanId", loanId.toString()));
        assertThat(prompt).contains("750");

        LoanPaymentResponse paymentResp = LoanPaymentResponse.builder()
                .id(UUID.randomUUID())
                .paymentAmount(BigDecimal.valueOf(750))
                .build();
        when(loanService.recordPayment(eq(loanId), any(), eq(userId))).thenReturn(paymentResp);

        ToolExecutionResult result = tool.execute(userId, Map.of(
                "loanId", loanId.toString(),
                "amount", "750.00"
        ));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).isEqualTo(paymentResp);

        // Missing parameters validation
        ToolExecutionResult missingAmount = tool.execute(userId, Map.of("loanId", loanId.toString()));
        assertThat(missingAmount.isSuccess()).isFalse();
    }
}
