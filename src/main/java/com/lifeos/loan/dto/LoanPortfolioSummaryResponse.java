package com.lifeos.loan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPortfolioSummaryResponse {

    private int activeLoansCount;
    private BigDecimal totalOriginalPrincipal;
    private BigDecimal totalOutstandingBalance;
    private BigDecimal totalMonthlyEmi;
    private BigDecimal totalPrincipalPaid;
    private BigDecimal totalInterestPaid;
}
