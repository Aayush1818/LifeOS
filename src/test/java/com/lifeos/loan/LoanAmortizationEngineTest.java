package com.lifeos.loan;

import com.lifeos.loan.engine.AmortizationInstallment;
import com.lifeos.loan.engine.AmortizationSchedule;
import com.lifeos.loan.engine.LoanAmortizationEngine;
import com.lifeos.loan.entity.PrepaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanAmortizationEngineTest {

    private LoanAmortizationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LoanAmortizationEngine();
    }

    @Test
    @DisplayName("Standard 30-year home loan: validates EMI, reconciliation and invariants")
    void standardHomeLoanSchedule() {
        BigDecimal principal = new BigDecimal("300000.00");
        BigDecimal rate = new BigDecimal("6.75");
        int tenureMonths = 360;
        LocalDate startDate = LocalDate.of(2026, 1, 1);

        BigDecimal emi = engine.calculateEmi(principal, rate, tenureMonths);
        // Formula verification: for 300,000 @ 6.75% for 360 months, standard EMI is ~1945.79
        assertThat(emi).isEqualByComparingTo("1945.79");

        AmortizationSchedule schedule = engine.generateSchedule(principal, rate, tenureMonths, startDate, 5);

        assertThat(schedule.getInstallments()).hasSize(360);
        assertThat(schedule.getMonthlyEmi()).isEqualByComparingTo("1945.79");

        // Invariant 1: Sum of principal components == original principal
        BigDecimal sumPrincipal = schedule.getInstallments().stream()
                .map(AmortizationInstallment::getPrincipalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumPrincipal).isEqualByComparingTo(principal);

        // Invariant 2: Closing principal after final installment == 0.00
        AmortizationInstallment last = schedule.getInstallments().get(359);
        assertThat(last.getClosingPrincipal()).isEqualByComparingTo("0.00");

        // Invariant 3: Total payments == principal + total interest
        BigDecimal sumPayments = schedule.getInstallments().stream()
                .map(AmortizationInstallment::getPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumPayments).isEqualByComparingTo(principal.add(schedule.getTotalInterest()));
    }

    @Test
    @DisplayName("Zero percent interest loan: exact linear division without interest")
    void zeroPercentInterestLoan() {
        BigDecimal principal = new BigDecimal("12000.00");
        BigDecimal rate = BigDecimal.ZERO;
        int tenureMonths = 12;
        LocalDate startDate = LocalDate.of(2026, 1, 1);

        BigDecimal emi = engine.calculateEmi(principal, rate, tenureMonths);
        assertThat(emi).isEqualByComparingTo("1000.00");

        AmortizationSchedule schedule = engine.generateSchedule(principal, rate, tenureMonths, startDate, 1);

        assertThat(schedule.getInstallments()).hasSize(12);
        assertThat(schedule.getTotalInterest()).isEqualByComparingTo("0.00");
        assertThat(schedule.getTotalPayment()).isEqualByComparingTo("12000.00");

        AmortizationInstallment last = schedule.getInstallments().get(11);
        assertThat(last.getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Rounding-heavy loan: final installment reconciles penny fractions perfectly")
    void roundingHeavyLoanSchedule() {
        BigDecimal principal = new BigDecimal("13456.78");
        BigDecimal rate = new BigDecimal("7.89");
        int tenureMonths = 23;
        LocalDate startDate = LocalDate.of(2026, 3, 15);

        AmortizationSchedule schedule = engine.generateSchedule(principal, rate, tenureMonths, startDate, 15);

        assertThat(schedule.getInstallments()).hasSize(23);

        BigDecimal sumPrincipal = schedule.getInstallments().stream()
                .map(AmortizationInstallment::getPrincipalComponent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumPrincipal).isEqualByComparingTo(principal);

        AmortizationInstallment last = schedule.getInstallments().get(22);
        assertThat(last.getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Prepayment with REDUCE_TENURE shortens tenure while maintaining EMI")
    void prepaymentReduceTenure() {
        BigDecimal originalPrincipal = new BigDecimal("100000.00");
        BigDecimal rate = new BigDecimal("6.00");
        int tenureMonths = 120; // 10 years
        BigDecimal originalEmi = engine.calculateEmi(originalPrincipal, rate, tenureMonths);

        // Prepayment after 12 months: remaining principal is 90,000; borrower prepays 30,000 -> 60,000 remaining
        BigDecimal remainingPrincipal = new BigDecimal("60000.00");
        int remainingTenure = 108;
        LocalDate nextDueDate = LocalDate.of(2027, 2, 1);

        AmortizationSchedule recalculated = engine.recalculateAfterPrepayment(
                remainingPrincipal, rate, remainingTenure, originalEmi,
                PrepaymentStrategy.REDUCE_TENURE, nextDueDate, 13
        );

        // Tenure should be noticeably shorter than 108 months
        assertThat(recalculated.getTotalTenureMonths()).isLessThan(108);
        assertThat(recalculated.getMonthlyEmi()).isEqualByComparingTo(originalEmi);

        // Reconciled to zero
        AmortizationInstallment last = recalculated.getInstallments().get(recalculated.getInstallments().size() - 1);
        assertThat(last.getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Prepayment with REDUCE_EMI keeps tenure but lowers monthly EMI")
    void prepaymentReduceEmi() {
        BigDecimal originalPrincipal = new BigDecimal("100000.00");
        BigDecimal rate = new BigDecimal("6.00");
        int tenureMonths = 120;
        BigDecimal originalEmi = engine.calculateEmi(originalPrincipal, rate, tenureMonths);

        BigDecimal remainingPrincipal = new BigDecimal("60000.00");
        int remainingTenure = 108;
        LocalDate nextDueDate = LocalDate.of(2027, 2, 1);

        AmortizationSchedule recalculated = engine.recalculateAfterPrepayment(
                remainingPrincipal, rate, remainingTenure, originalEmi,
                PrepaymentStrategy.REDUCE_EMI, nextDueDate, 13
        );

        // Tenure preserved
        assertThat(recalculated.getTotalTenureMonths()).isEqualTo(108);
        // EMI lowered
        assertThat(recalculated.getMonthlyEmi()).isLessThan(originalEmi);

        AmortizationInstallment last = recalculated.getInstallments().get(107);
        assertThat(last.getClosingPrincipal()).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-100.00", "0.00"})
    @DisplayName("Validation fails on zero or negative principal")
    void invalidPrincipalThrows(String amount) {
        assertThatThrownBy(() -> engine.calculateEmi(new BigDecimal(amount), new BigDecimal("5.0"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Validation fails on negative interest rate or rate > 100")
    void invalidInterestRateThrows() {
        assertThatThrownBy(() -> engine.calculateEmi(new BigDecimal("1000.00"), new BigDecimal("-1.0"), 12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> engine.calculateEmi(new BigDecimal("1000.00"), new BigDecimal("101.0"), 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Validation fails on zero or negative tenure")
    void invalidTenureThrows() {
        assertThatThrownBy(() -> engine.calculateEmi(new BigDecimal("1000.00"), new BigDecimal("5.0"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
