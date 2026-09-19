package com.lifeos.loan.repository;

import com.lifeos.loan.dto.LoanPortfolioSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoanAnalyticsJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public LoanPortfolioSummaryResponse calculatePortfolioSummary(UUID userId) {
        String sql = """
            SELECT 
                COUNT(*) as active_count,
                COALESCE(SUM(principal_amount), 0) as total_principal,
                COALESCE(SUM(outstanding_balance), 0) as total_outstanding,
                COALESCE(SUM(monthly_emi), 0) as total_emi,
                COALESCE(SUM(total_principal_paid), 0) as total_principal_paid,
                COALESCE(SUM(total_interest_paid), 0) as total_interest_paid
            FROM loans
            WHERE user_id = :userId
              AND status = 'ACTIVE'
              AND is_deleted = false
        """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> LoanPortfolioSummaryResponse.builder()
                .activeLoansCount(rs.getInt("active_count"))
                .totalOriginalPrincipal(rs.getBigDecimal("total_principal"))
                .totalOutstandingBalance(rs.getBigDecimal("total_outstanding"))
                .totalMonthlyEmi(rs.getBigDecimal("total_emi"))
                .totalPrincipalPaid(rs.getBigDecimal("total_principal_paid"))
                .totalInterestPaid(rs.getBigDecimal("total_interest_paid"))
                .build());
    }
}
