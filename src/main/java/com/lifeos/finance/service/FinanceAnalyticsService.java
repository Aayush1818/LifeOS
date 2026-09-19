package com.lifeos.finance.service;

import com.lifeos.finance.dto.CategoryExpenseResponse;
import com.lifeos.finance.dto.MonthOverMonthResponse;
import com.lifeos.finance.dto.MonthlySummaryResponse;
import com.lifeos.finance.repository.FinanceAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service providing deterministic financial aggregations and month-over-month trend analytics.
 * Employs direct JDBC query acceleration for high-speed calculation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceAnalyticsService {

    private final FinanceAnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(UUID userId, int month, int year) {
        YearMonth targetPeriod = YearMonth.of(year, month);
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();

        FinanceAnalyticsRepository.MonthlyTotals totals = analyticsRepository.calculateMonthlyTotals(userId, startDate, endDate);
        BigDecimal totalIncome = totals.totalIncome();
        BigDecimal totalExpenses = totals.totalExpenses();
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        BigDecimal savingsRate = totalIncome.compareTo(BigDecimal.ZERO) > 0
                ? netSavings.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<FinanceAnalyticsRepository.CategorySpend> breakdownList = analyticsRepository.getCategoryExpenseBreakdown(userId, startDate, endDate);
        List<CategoryExpenseResponse> categoryResponses = new ArrayList<>();

        for (FinanceAnalyticsRepository.CategorySpend cat : breakdownList) {
            BigDecimal percentage = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? cat.amount().multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            categoryResponses.add(CategoryExpenseResponse.builder()
                    .category(cat.category())
                    .amount(cat.amount())
                    .percentageOfTotal(percentage)
                    .transactionCount(cat.count())
                    .build());
        }

        MonthOverMonthResponse momComparison = calculateMonthOverMonth(userId, targetPeriod, totalExpenses);

        log.debug("Calculated monthly summary for user [{}] in {}/{}: Income={}, Expenses={}, Savings={}",
                userId, month, year, totalIncome, totalExpenses, netSavings);

        return MonthlySummaryResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .savingsRatePercentage(savingsRate)
                .recurringExpensesTotal(totals.recurringExpenses())
                .categoryBreakdown(categoryResponses)
                .priorMonthComparison(momComparison)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseResponse> getCategoryBreakdown(UUID userId, int month, int year) {
        return getMonthlySummary(userId, month, year).getCategoryBreakdown();
    }

    @Transactional(readOnly = true)
    public MonthOverMonthResponse getMonthOverMonthComparison(UUID userId, int month, int year) {
        YearMonth targetPeriod = YearMonth.of(year, month);
        LocalDate startDate = targetPeriod.atDay(1);
        LocalDate endDate = targetPeriod.atEndOfMonth();
        BigDecimal currentExpenses = analyticsRepository.getTotalExpensesForPeriod(userId, startDate, endDate);

        return calculateMonthOverMonth(userId, targetPeriod, currentExpenses);
    }

    private MonthOverMonthResponse calculateMonthOverMonth(UUID userId, YearMonth currentPeriod, BigDecimal currentExpenses) {
        YearMonth priorPeriod = currentPeriod.minusMonths(1);
        LocalDate priorStart = priorPeriod.atDay(1);
        LocalDate priorEnd = priorPeriod.atEndOfMonth();

        BigDecimal priorExpenses = analyticsRepository.getTotalExpensesForPeriod(userId, priorStart, priorEnd);
        BigDecimal delta = currentExpenses.subtract(priorExpenses);

        BigDecimal percentageChange;
        String direction;

        if (priorExpenses.compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = delta.abs().multiply(BigDecimal.valueOf(100))
                    .divide(priorExpenses, 2, RoundingMode.HALF_UP);
        } else {
            percentageChange = currentExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf(100.00)
                    : BigDecimal.ZERO;
        }

        int comparison = delta.compareTo(BigDecimal.ZERO);
        if (comparison > 0) {
            direction = "INCREASED";
        } else if (comparison < 0) {
            direction = "DECREASED";
        } else {
            direction = "UNCHANGED";
        }

        return MonthOverMonthResponse.builder()
                .currentMonthExpenses(currentExpenses)
                .priorMonthExpenses(priorExpenses)
                .deltaAmount(delta)
                .percentageChange(percentageChange)
                .direction(direction)
                .build();
    }
}
