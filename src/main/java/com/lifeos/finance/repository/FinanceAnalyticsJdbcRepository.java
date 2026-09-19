package com.lifeos.finance.repository;

import com.lifeos.finance.entity.TransactionCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Direct Spring JDBC implementation for aggregation and reporting queries.
 *
 * ARCHITECTURAL RATIONALE FOR JDBC OVER JPA:
 * 1. Database Pushdown: Calculations such as conditional SUM, GROUP BY, and refund deductions
 *    are performed directly inside the PostgreSQL query engine.
 * 2. Zero Hydration Overhead: Bypasses Hibernate 1st-level cache and entity object instantiation,
 *    reducing GC pressure and execution time by up to 90% over large transaction histories.
 * 3. Exact Precision: Preserves PostgreSQL numeric decimal precision directly into java.math.BigDecimal.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FinanceAnalyticsJdbcRepository implements FinanceAnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public MonthlyTotals calculateMonthlyTotals(UUID userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' AND status = 'POSTED' THEN amount ELSE 0 END), 0) AS total_income,
                COALESCE(SUM(CASE 
                    WHEN transaction_type = 'EXPENSE' AND status = 'POSTED' AND is_refund = false THEN amount
                    WHEN transaction_type = 'EXPENSE' AND status = 'POSTED' AND is_refund = true THEN -amount
                    ELSE 0 END), 0) AS total_expenses,
                COALESCE(SUM(CASE 
                    WHEN is_recurring = true AND status = 'POSTED' AND transaction_type = 'EXPENSE' THEN amount 
                    ELSE 0 END), 0) AS recurring_expenses
            FROM transactions
            WHERE user_id = :userId
              AND transaction_date >= :startDate
              AND transaction_date <= :endDate
              AND is_deleted = false
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> new MonthlyTotals(
                rs.getBigDecimal("total_income"),
                rs.getBigDecimal("total_expenses"),
                rs.getBigDecimal("recurring_expenses")
        ));
    }

    @Override
    public List<CategorySpend> getCategoryExpenseBreakdown(UUID userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                category,
                SUM(CASE WHEN is_refund = true THEN -amount ELSE amount END) AS total_amount,
                COUNT(*) AS tx_count
            FROM transactions
            WHERE user_id = :userId
              AND transaction_type = 'EXPENSE'
              AND status = 'POSTED'
              AND transaction_date >= :startDate
              AND transaction_date <= :endDate
              AND is_deleted = false
            GROUP BY category
            HAVING SUM(CASE WHEN is_refund = true THEN -amount ELSE amount END) > 0
            ORDER BY total_amount DESC
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String catStr = rs.getString("category");
            TransactionCategory category;
            try {
                category = TransactionCategory.valueOf(catStr);
            } catch (IllegalArgumentException e) {
                category = TransactionCategory.OTHER;
            }
            return new CategorySpend(
                    category,
                    rs.getBigDecimal("total_amount"),
                    rs.getLong("tx_count")
            );
        });
    }

    @Override
    public Map<TransactionCategory, BigDecimal> getActualSpendingPerCategory(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<CategorySpend> list = getCategoryExpenseBreakdown(userId, startDate, endDate);
        Map<TransactionCategory, BigDecimal> spendMap = new EnumMap<>(TransactionCategory.class);
        for (CategorySpend item : list) {
            spendMap.put(item.category(), item.amount());
        }
        return spendMap;
    }

    @Override
    public BigDecimal getTotalExpensesForPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COALESCE(SUM(CASE 
                    WHEN transaction_type = 'EXPENSE' AND status = 'POSTED' AND is_refund = false THEN amount
                    WHEN transaction_type = 'EXPENSE' AND status = 'POSTED' AND is_refund = true THEN -amount
                    ELSE 0 END), 0) AS total_expenses
            FROM transactions
            WHERE user_id = :userId
              AND transaction_date >= :startDate
              AND transaction_date <= :endDate
              AND is_deleted = false
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return result != null ? result : BigDecimal.ZERO;
    }
}
