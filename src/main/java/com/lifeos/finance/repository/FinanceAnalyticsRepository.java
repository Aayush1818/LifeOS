package com.lifeos.finance.repository;

import com.lifeos.finance.entity.TransactionCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance direct JDBC aggregation contract for financial reporting,
 * summary calculations, and category breakdowns.
 */
public interface FinanceAnalyticsRepository {

    record MonthlyTotals(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal recurringExpenses
    ) {}

    record CategorySpend(
            TransactionCategory category,
            BigDecimal amount,
            long count
    ) {}

    /**
     * Pushes calculation of total income, net expenses (accounting for refunds),
     * and recurring expenses to PostgreSQL engine.
     */
    MonthlyTotals calculateMonthlyTotals(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Executes database GROUP BY category calculation sorted by spend descending.
     */
    List<CategorySpend> getCategoryExpenseBreakdown(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Returns a map of category to net actual expenses for budget tracking.
     */
    Map<TransactionCategory, BigDecimal> getActualSpendingPerCategory(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Returns total net expense for prior month comparison.
     */
    BigDecimal getTotalExpensesForPeriod(UUID userId, LocalDate startDate, LocalDate endDate);
}
