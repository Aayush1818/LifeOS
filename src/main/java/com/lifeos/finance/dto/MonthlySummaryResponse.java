package com.lifeos.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {

    private int month;
    private int year;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private BigDecimal savingsRatePercentage;
    private BigDecimal recurringExpensesTotal;
    @Builder.Default
    private List<CategoryExpenseResponse> categoryBreakdown = new ArrayList<>();
    private MonthOverMonthResponse priorMonthComparison;
}
